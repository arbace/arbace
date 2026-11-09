# Scheme forms: the spec of repr/scheme (SM1)

Status: SM1, 2026-10-30. The spec of Scheme as Arbace forms for R6RS (SC1 (a)): the lexical syntax and the core
language. The decisions are doc/scheme/SCHEME-SURVEY.md's SC1-SC14 as recommended, "Decided (2026-10-30)"; what
this spec had to choose beyond them is SC20 onward ("As built: SM1, SM2"). The reader and the printer
(`arbace.repr.scheme`) are SM2's and follow this text; `bin/repr-scheme spec-check` reads the examples below with
the reader and requires the forms equal.

Words: *Scheme forms* are Scheme data spelled so that Arbace's reader (the Clojure reader, `read`, not strict EDN)
reads them back to the same datum; *the reference* is Chez Scheme 10.3.0 in R6RS mode; *a datum* is what Scheme's
`read` returns.

## 1. What a form is

A Scheme program is a sequence of data. A file's forms are its top-level data in order, each a *form*; nothing is
wrapped, and a form says nothing of what it means: `(define x 1)` is a list of two symbols and a number, as in
Scheme. Meaning is the expander's and the evaluator's (SM4, SM5), which read forms as data and tell code from
datum by context, as Scheme does.

The forms are held in `.ae` files, one top-level datum a line group, each form readable by `read`. A form is one of:

| a Scheme datum | its form |
|---|---|
| a boolean | `true`, `false` |
| the empty list | `()` |
| a proper list | a list |
| an improper list `(a b . c)` | `(scm/dot (a b) c)`: the proper prefix, then the tail |
| a vector | a vector `[...]` |
| a bytevector | `(scm/bytevector 1 2 3)` |
| a character | a Clojure character, or `(scm/char N)` |
| a string | a string |
| a symbol | a Clojure symbol, or `(scm/sym "name")` |
| an exact integer | a long, or a BigInt (`123N`) |
| an exact rational | a ratio `1/2` |
| a flonum | a double, or `(scm/flonum "+inf.0")` |
| a complex number | `(scm/complex RE IM)` |

Nothing else is a form: `nil` is no Scheme datum, a Clojure map, set, keyword or regex is none either. A list
whose first element is the symbol `scm/dot`, `scm/sym`, `scm/char`, `scm/flonum`, `scm/complex` or
`scm/bytevector` is one of the spellings above and never a Scheme list, because a Scheme symbol that contains
`/` is spelled `(scm/sym "...")` (3.2), so no Scheme list starts with a bare symbol of that form.

## 2. The reader's job

`repr/scheme read` takes R6RS lexical syntax (report chapter 4, appendix) as Chez reads it in its R6RS mode, and
gives forms. The comments are not data and are dropped in `:lines`: `;` to the end of the line, nested `#| |#`,
`#;` and the datum after it. So are the directives `#!r6rs`, `#!fold-case`, `#!no-fold-case` and `#!chezscheme`, which act
on reading (`#!fold-case` folds the case of identifiers and character names, 12; `#!r6rs` selects the mode, 6;
`#!chezscheme` is taken and changes nothing, 12). `[` `]` read as the list
brackets `( )`. `:full` (SM3) will keep every one of these in a layer beside the forms.

Examples (Scheme text, then its forms; `bin/repr-scheme spec-check` reads the first with the reader and compares
with the second read by `read`):

```scheme
(define (f x) (if (< x 2) x (+ (f (- x 1)) (f (- x 2)))))
```
```forms
(define (f x) (if (< x 2) x (+ (f (- x 1)) (f (- x 2)))))
```

```scheme
'(a . b) '(1 2 . 3) '() `(x ,y ,@z)
```
```forms
(quote (scm/dot (a) b)) (quote (scm/dot (1 2) 3)) (quote ()) (quasiquote (x (unquote y) (unquote-splicing z)))
```

```scheme
#(1 #(2)) #vu8(0 255) "tab\there" #\a #\space #\x41 #\x1F600
```
```forms
[1 [2]] (scm/bytevector 0 255) "tab\there" \a \space \A (scm/char 128512)
```

```scheme
[let ([x 1]) x] #;(hidden) visible #| block #| nested |# |# next
```
```forms
(let ((x 1)) x) visible next
```

## 3. The spellings

### 3.1 Quotation and syntax quotation

`'x` `` `x `` `,x` `,@x` are `(quote x)`, `(quasiquote x)`, `(unquote x)`, `(unquote-splicing x)`, plain lists.
`#'x` `` #`x `` `#,x` `#,@x` are `(syntax x)`, `(quasisyntax x)`, `(unsyntax x)`, `(unsyntax-splicing x)`. The
reader abbreviates nothing back (Chez's `write` prints `(quote a)`, 7).

### 3.2 Symbols

Scheme symbols are case sensitive. The symbol named `name` is the Clojure symbol `name` when its characters are
all ASCII letters, digits or one of `! $ & * + - . < = > ? _`, its first character is not a digit, it does not
start with `+`, `-` or `.` followed by a digit, it is not `.` alone and not `nil`, `true` or `false`; the
Clojure reader then reads it back as that symbol (the check validates the rule against the reader). Every other
symbol is `(scm/sym "name")`: `a/b`, `a:b`, `1+`, `λ`, `a b` (written `a\x20;b` in R6RS), the empty symbol
`(scm/sym "")`. R6RS's `...`, `+`, `-`, `->x` are bare.

```scheme
ABC ... -> ->x + - a\x20;b \x31;23 lambda
```
```forms
ABC ... -> ->x + - (scm/sym "a b") (scm/sym "123") lambda
```

### 3.3 Characters and strings

A character with scalar value below U+10000 is a Clojure character; above, `(scm/char N)` with N decimal. A
Scheme string is a string of the same characters; a character above U+FFFF is its UTF-16 pair in the form (the
runtime's strings are runes, SC8 (a): the conversion is the evaluator's boundary). A character is never a
surrogate (R6RS: a scalar value), in strings or as `#\x`.

Characters are read by name (`nul alarm backspace tab linefeed newline vtab page return esc space delete`,
case sensitive), by `#\xHEX`, or as the character after `#\`; strings by the escapes `\a \b \t \n \v \f \r \"
\\`, `\xHEX;` and the line continuation `\<intraline whitespace>*<line ending><intraline whitespace>*`.

### 3.4 Numbers

Exact integers are longs, or BigInts beyond a long (`12345678901234567890N`); exact rationals are normalized
ratios (`6/4` is `3/2`, `4/2` is `2`); flonums are doubles. `+inf.0`, `-inf.0` and `+nan.0` have no Clojure
spelling: `(scm/flonum "+inf.0")`. `-0.0` is Clojure's `-0.0` (the comparison tells it from `0.0`). Prefixes
`#x #b #o #d #e #i` (either order) are read and the value is the form: `#e1.5` is `3/2`, `#x-ff` is `-255`,
`#i1/3` is `0.3333333333333333`.

A complex number whose imaginary part is not an exact zero is `(scm/complex RE IM)` with both parts exact or both
flonums, as Chez holds them (`1+2.5i` reads as `1.0+2.5i`); `1+0i` is the real `1`, `+i` is `(scm/complex 0 1)`,
`+inf.0i` is `(scm/complex 0.0 (scm/flonum "+inf.0"))`. A polar number `r@a` is read to rectangular (inexact)
unless `a` is an exact zero. Complex numbers are read here and deferred as a class in the evaluator (SC7 (a)).

```scheme
1/2 6/4 -3 #xff #e1.5 1e3 -0.0 +inf.0 1+2i 1.5-2.5i +i 12345678901234567890
```
```forms
1/2 3/2 -3 255 3/2 1000.0 -0.0 (scm/flonum "+inf.0") (scm/complex 1 2) (scm/complex 1.5 -2.5) (scm/complex 0 1) 12345678901234567890N
```

### 3.5 Booleans

`#t #f #true #false` (and `#T #F`) are `true` and `false`. The empty list is `()`, never `nil`.

### 3.6 Nothing the reader cannot say

Datum labels (`#0=`), `#u8(` and the Chez-specific `#!eof`, `#!bwp`, `#{...}`, `|a b|` symbols, `{`, `#[`, `#%`
are refused in R6RS mode (6); the cyclic data they make are never forms.

## 4. Positions

`read` keeps, beside each top-level datum, its start and end offset in the text (Chez's `bfp` and `efp`: counted
in characters, scalar values, not UTF-16 units). In `:lines` positions are the reader's result beside the forms
(`{:off [start end] :form f}`), not inside them: metadata on lists would enter the equality of data (SC12 (a)).
A nested datum's position is SM3's (`:full`).

## 5. The core language as forms

The expander (SM4) produces, and the evaluator (SM5) and the AOT (SM7) consume, forms of the core language. Since
SM4 they are what psyntax's builders build (`scheme/psyntax/builders.ae` with Arbace's options, `config.ae` and
`patches.edn`: no annotations, `case-lambda` for every procedure, `letrec*`, no `library-letrec*`, no global
`define`), as `bin/si expand` prints them; doc/scheme/SCHEME-SURVEY.md, "As built: SM4".

| core form | shape |
|---|---|
| constant | `(quote d)` (psyntax quotes every constant; a self-evaluating datum unquoted is a constant too) |
| reference | a symbol: a lexical variable when a binder above binds it, else a location (a library's global) |
| primitive | `(primitive name)`: the procedure the host's runtime provides as `name` (`car`, `+`, `make-record-type-descriptor`) |
| `case-lambda` | `(case-lambda (FORMALS body) ...)`, FORMALS a list of symbols, a symbol or `(scm/dot (a b) rest)`; also each `lambda` |
| `if` | `(if test then else)` |
| `set!` | `(set! id expr)`, a lexical variable or a location |
| `begin` | `(begin expr ...)` |
| `letrec`, `letrec*` | `(letrec ((id init) ...) body)`, `(letrec* ...)` |
| application | `(operator operand ...)` |

The unspecified value is `((primitive void))`. A top-level program is one `letrec*` whose bindings are its
definitions and, for each expression between them, a binding of a fresh variable (`dummy.N`); a library's invoke
code binds its definitions by `letrec*` and assigns each export's location by `set!`. Lexical variables are
gensyms, printed `NAME.N` (numbered in the order met, never the name of a free symbol of the form; SC43).

A top-level program is `(import ...)` then body forms; a library is `(library (name version?) (export ...)
(import ...) body ...)`, with the report's export and import specs (`rename`, `only`, `prefix`, `for`). Derived
forms (`define`, `let`, `let*`, `do`, `case`, `cond`, `and`, `or`, `when`, `unless`, `let-values`,
`define-record-type`, `guard`, `parameterize` as Chez has it) stay as written in the forms and are the
expander's to expand; there is no node kind for them here.

```scheme
(library (counter (1))
  (export make-counter)
  (import (rnrs))
  (define (make-counter)
    (let ([n 0]) (lambda () (set! n (+ n 1)) n))))
```
```forms
(library (counter (1))
  (export make-counter)
  (import (rnrs))
  (define (make-counter)
    (let ((n 0)) (lambda () (set! n (+ n 1)) n))))
```

```scheme
(define-record-type point (fields (immutable x) (mutable y)))
(let loop ([i 0] [acc '()]) (if (= i 3) (reverse acc) (loop (+ i 1) (cons i acc))))
```
```forms
(define-record-type point (fields (immutable x) (mutable y)))
(let loop ((i 0) (acc (quote ()))) (if (= i 3) (reverse acc) (loop (+ i 1) (cons i acc))))
```

```scheme
(case-lambda [() 0] [(x) x] [(x . rest) (apply + x rest)])
```
```forms
(case-lambda (() 0) ((x) x) ((scm/dot (x) rest) (apply + x rest)))
```

## 6. The mode of the reference

Chez has two readers: its default one, which accepts extensions (`|a b|`, `{`, `#!eof`, `1+` and `+a` as
symbols, `#3(a)`, datum labels), and the R6RS reader that `#!r6rs` selects, which refuses them. This spec's
reader is the second: the check reads every file in Chez after a `#!r6rs` directive (the dump takes the seven
characters off its offsets), so a file with an extension is *refused* by both, and the refusal is judged by
class (8). Chez's default mode is a possible later layer (SC20).

In R6RS mode the delimiters of a token are whitespace, `( ) [ ] " ;` and `#`; the characters `' ` , { } |` inside
a token are errors; an identifier is an `<initial>` then `<subsequent>*` or one of the peculiar identifiers `+`
`-` `...` `->...`, with inline hex escapes `\xHEX;` anywhere (and the `;` of an escape is no comment); initials
and subsequents are the report's, over the general categories of the Unicode Character Database 16.0.0 (since
SCS9 (c): `arbace/repr/scheme/unicode.ae`, generated by `bin/repr-scheme ucd gen` from the database pinned by
`test/scheme/ucd.edn`; before, Chez's answers, the same ranges), checked against the report's classes by an
exhaustive scan of the scalar values (9). So is the whitespace, R6RS 4.2.1's `<whitespace>` (tab, line feed, line
tabulation, form feed, carriage return, next line, Zs, Zl, Zp) and `<intraline whitespace>` (tab, Zs) in a string's line
continuation (since SCS13 (b), the same file and si's `go/arbace/scm/unicode.ae`; before, Java's `Character/getType` and
Go's `unicode` tables, equal to it), checked by the whitespace scan (9).

## 7. The printer, `:lines`

`repr/scheme print` writes forms as Scheme text: Chez's `write` of each datum, one datum a line, with no layout of
its own; `--layout` (11) lays them out. Chez's choices, which the check compares to the byte:

- strings: `\"` `\\` and the escapes `\a \b \t \n \v \f \r`; every other character below U+20, U+7F, U+85 and U+2028 as
  `\xHEX;` (upper case); everything else as it is;
- characters: `#\nul #\alarm #\backspace #\tab #\newline #\vtab #\page #\return #\esc #\space #\delete`, `#\xHEX`
  for 1-6, E-1A, 1C-1F, 85, 2028, otherwise the character itself;
- symbols: the characters that are not `<subsequent>` (`<initial>` first) as `\xHEX;`, so `a b` is `a\x20;b` and `-a`
  is `\x2D;a`; `||` for the empty symbol; `+`, `-`, `...` and `->x` as they are;
- numbers: integers and ratios in decimal; flonums by the shortest digits that read back (Java's, with Chez's
  layout): positional from 0.001 to below 1e10 (`0.001`, `1000000000.0`), else exponent form without a sign or
  padding (`1e10`, `1.5e-7`), a subnormal with `|` and its bits of precision (`5e-324|1`); `+inf.0 -inf.0 +nan.0`;
  complex `RE+IMi` with the sign of the imaginary part always written (`0+1i`);
- `#t #f`, `#(a b)`, `#vu8(1 2)`, a quote form as the list `(quote a)`, an improper list `(a . b)`.

Since SCS17 (c) (the user, 2026-11-09) the escapes are si's stated rules, each with an id, over the Unicode Character
Database 16.0.0's categories (R6RS 4.2.7 gives `write` none; these are Chez Scheme 10.3.0's choices, measured equal over
every scalar value, and what si's printer and repr/scheme's write):

| id | the rule |
|---|---|
| `:print-string` | in a string, `"` and `\` are written `\"` and `\\`, U+0007 to U+000D by their mnemonic escapes `\a \b \t \n \v \f \r`, and every other character that is a control of the ASCII range (a character of Cc below U+0080: U+0000 to U+001F, U+007F) or starts a `<line ending>` (R6RS 4.2.1: U+0085 next line, U+2028 line separator) as `\xHEX;`, HEX the scalar value in upper case without leading zeros; every other character as itself (U+2029, Zp, is no `<line ending>`: as itself; U+0080 to U+009F, Cc, as themselves) |
| `:print-char` | a character is written by its name when it has one (`#\nul #\alarm #\backspace #\tab #\newline #\vtab #\page #\return #\esc #\space #\delete`), else as `#\xHEX` when it is a control of the ASCII range or starts a `<line ending>`, else as `#\` and itself |
| `:print-symbol` | a symbol's name is written as it is, but each character that is no `<subsequent>` (the first: no `<initial>`) as `\xHEX;`, by the reader's classes (6, generated from the same database); a `<peculiar identifier>` (`+`, `-`, `...`, `->` and what follows it, its characters `<subsequent>`) as it is; the empty name as `\|\|` |

The tables are generated from the rules by `bin/repr-scheme ucd gen` (`arbace.repr.scheme.ucd`: `arbace/repr/scheme/unicode.ae`'s
`string-hex?` and `char-hex?`, si's `unicode.ae`'s `IsStringHex` and `IsCharHex`), and the check judges the four printing scans
(9) by the rules stated a second time in `arbace.repr.scheme.report` (`written-escaped-in-string?`, `written-hex-char?`,
`written-escaped-symbol-first?`, `written-escaped-symbol-rest?`), no dump read; `bin/repr-scheme report-scans` (by hand) holds
the rules to Chez's recorded scans and reads each written text back by the report's grammar.

## 8. Errors

A text Chez refuses is refused. The reader's error is `{:class K :message S :at N}`; the classes are those of
`arbace.repr.scheme.check/chez-classes`: `:unexpected-close`, `:unexpected-eof`, `:bad-dot`, `:invalid-sharp`,
`:bad-char-name`, `:bad-string-escape`, `:bad-number`, `:bad-bytevector`, `:bad-symbol`. The check requires Chez's
message to fall in the same class as the reader's error; the data before the error are compared too. Messages
and offsets of an error are informational (Chez reports positions its own way).

## 9. The oracle and the check

`tools/schemedump/dump.ss` (Chez 10.3.0, `chez --script`) reads files after `#!r6rs` with Chez's own `read` and
writes one map a file: `{:file F :mode :r6rs :data [{:off [S E] :form FORM :write "TEXT"} ...] :error {...}}`.
FORM is the datum spelled in the forms of 1-3 by an independent implementation in Scheme; TEXT is Chez's `write`.
`--scan KIND` writes the ranges of the code points for which a lexical or printing predicate holds
(`whitespace`, `ident-initial`, `ident-subsequent`, `string-escaped`, `char-hex`, `symbol-escaped-first`,
`symbol-escaped-rest`).

`bin/repr-scheme check` compares, per file or probe: E-tree (the forms equal, lists apart from vectors, exact
from inexact, flonums by bits), E-off (the datum's offsets), E-write (the printer's text equal to Chez's), E-error
(the same class), and per scan the ranges equal. A difference not in `test/scheme/check-known.edn` fails, and so
does a listed one that no longer occurs.

Since SCS5 (b) a probe is judged by the report instead of Chez's dump wherever the report decides it:
`arbace.repr.scheme.report` reads the text by R6RS 4.1-4.3's grammar rule for rule, apart from the reader, its numbers
by exact arithmetic (an inexact one the binary64 nearest its exact value, ties to even, by its own conversion), and the
check compares Arbace's reading with it: E-tree, E-off, the count, E-error (an error where the grammar has no
production; a run that is no lexeme any of the token classes) and E-write as a property (each datum written and read
back the same datum). The report leaves `#!<identifier>` (4.1), an exact ratio with a zero denominator, `#e` on an
infinity or a NaN, a mantissa width below 53 or with `#e`, a complex number of mixed exactness and a polar number but
for an exact zero angle to the implementation: those probes, and the 37 where Chez's R6RS mode reads past the grammar
(`test/scheme/report-chez.edn`), are judged by their dump. `bin/repr-scheme report-check` compares the three readings.

Since SCS7 (c) a corpus file is judged the same way (`arbace.repr.scheme.check/judge-file`): by the report wherever it
decides the whole text, else (a datum it leaves to the implementation, or a rule of `test/scheme/report-chez.edn` naming the
file) by its dump. Since SCS9 (c) the three reading scans (`whitespace`, `ident-initial`, `ident-subsequent`) are judged
by the report's character classes over the Unicode Character Database 16.0.0 (`arbace/repr/scheme/category.ae`,
generated from the pinned database), no dump read; the four printing scans stay judged by their dumps (`write`'s
choices are the implementation's). `bin/repr-scheme report-scans` compares the report's classes with Chez's recorded
scans (equal: Chez 10.3.0's data are 16.0's; `test/scheme/report-scans.edn`). Since SCS10 (b) the report applies si's
choices (12) where R6RS defers, so the probes and files they decide are judged without a dump too; the polar numbers
stay on their dumps.

## 10. The corpora

`test/scheme/corpora.edn`: Racket's R6RS tests (`racket/r6rs`, `r6rs-test/tests`, Apache-2.0 OR MIT), Larceny's
`test/R6RS` (`.sch`), Chibi Scheme's R7RS tests (BSD-3-Clause; the file is R7RS, which Chez refuses: the corpus
of refusals, and SM8's suite) and go-whim's whimsical (read in place, pinned by tree hash), each file pinned by
SHA-256; fetched by `bin/repr-scheme corpus fetch` into `.tmp/scheme/corpora/`. The probes
(`test/scheme/probes.edn`): SCHEME-SURVEY 2.4's, batteries of tokens (prefixes by bodies of numbers, the report's
character names, delimiters, comments, dots), 3,000 random numbers and exhaustive character classes.

## 11. `:full` and the layout printer

**`:full`** keeps what the forms leave out, so that the text prints back byte for byte. The forms are those of
`:lines` (1-3); beside them is the *layer*, a tree of nodes parallel to the forms, one per datum, `(arbace.repr.scheme.read/*full*)`:

| node | for | holds |
|---|---|---|
| `[pre sp]` | an atom (symbol, number, boolean, character, string) | `pre`, the atmosphere before it; `sp`, its spelling when that is not Chez's `write` of the form (`#true`, `#\x41`, `1.50`, `"s\x41;t"`, `Foo` under `#!fold-case`), else `nil` |
| `[pre :q prefix inner]` | a quote form | the prefix text (`'` `` ` `` `,` `,@` `#'` `` #` `` `#,` `#,@`), the node of the datum after it |
| `[pre :l open kids close-pre close]` | a list, vector or bytevector | `open` is `(`, `[`, `#(` or `#vu8(`; `kids`, one node a datum; the atmosphere before the closer and the closer |
| `[pre :d open kids dot-pre tail close-pre close]` | a dotted list | the nodes before the dot, the atmosphere before the dot, the node of what follows it (a list, when the tail was spliced: `(a . (b c))`), the closer |

*Atmosphere* is the text between data: whitespace, `;` comments, nested `#| |#` comments, `#;` with the datum it
comments out, and the directives `#!r6rs`, `#!fold-case`, `#!no-fold-case`, all as text. The layer ends with the
text after the last datum: all of the remainder when the reader stopped at an error, so a refused text also prints
back. Printing walks the forms and the layer together: atmosphere, then the spelling or `write` of the atom, then
the brackets, and for a dotted node the remaining items rebuilt as the reader would have formed them.

The `.ae` file of a text in `:full` is its forms, one a line as `read` writes them, then one last line
`#_(scm/layer {:nodes [...] :end "..."})`, which the Clojure reader skips: a file in `:full` reads as the `:lines`
forms. `read --full`, `print --full`.

**The layout printer** (`print --layout`) writes forms as a person writes Scheme, with no comments. A datum that
fits in 80 columns is one line; quote forms are abbreviated (`'x` `` `x `` `,x` `,@x` `#'x`), which reads back as
the same forms. A datum that does not fit is broken by its head: `define`, `lambda`, `let` (two arguments for a named
let), `let*`, `letrec`, `letrec*`, `let-values`, `when`, `unless`, `case`, `guard`, `parameterize`, `do` (two),
`syntax-case` (two), `library`, `define-syntax`, `define-record-type`, `syntax-rules` and the others of
`arbace.repr.scheme.layout/body-forms` keep that many arguments on the line and indent the rest by two; any other
call with a symbol head up to 18 characters aligns its arguments under the first one (longer: indented by two); a
list headed by a non-symbol aligns its items under its first; a vector or bytevector of atoms is filled to the
width. A dotted tail is a line of its own, `. tail`. Top-level data are separated by a newline, and a blank line
when either takes more than one line. The reading of the layout is the same forms (`tree=`), and the layout of
those forms is the same text (idempotent): both are checked by `roundtrip`.

## 12. si's choices where the report defers

R6RS leaves some readings to the implementation (4.1, 4.2.8, 11.7.4.1). si's reader makes these choices, stated
as rules that `arbace.repr.scheme.report` applies (SCS10 (b); each has an id, which `read-all` reports as
`:choices`; with `*choices*` bound false the report alone leaves them undecided):

| id | where R6RS defers | si's choice |
|---|---|---|
| `:mixed-exactness` | a complex number whose parts differ in exactness (`1+2.5i`; an exact zero imaginary part makes it a real, the report's rule) | both parts inexact, an exact part its nearest binary64 (si's tower, SM5): `1.0+2.5i`; `+inf.0i` is `0.0+inf.0i` |
| `:ratio-over-zero` | an exact ratio with a zero denominator | exact (`1/0`, `#e1/0`, `#x1/0`): no number, so a syntax violation (the run is no lexeme); inexact (`#i1/0`): the binary64 quotient, `+inf.0` or `-inf.0` by the numerator's sign, `+nan.0` for `0/0` |
| `:exact-naninf` | `#e` on an infinity or a NaN (no exact equivalent, 11.7.4.1) | no number, a syntax violation (`#e+inf.0`, and a complex number with such a part) |
| `:mantissa-width` | a mantissa width below 53, or with `#e` | read and ignored: si's flonums have 53 bits, the greater precision 4.2.8 allows (`1.5\|10` is `1.5`); with `#e` the exact value of the digits (`#e1.5\|53` is `3/2`) |
| `:fold-case` | `#!fold-case`, `#!no-fold-case` (4.1's `#!<identifier>`) | directives, as R7RS 2.1 names them: after `#!fold-case`, until `#!no-fold-case`, the identifiers and the character names are folded by full case folding (since SCS12 (c): the Unicode Character Database 16.0.0's `CaseFolding.txt`, its C and F mappings, the Turkic T left out, as R6RS's `string-foldcase` and Chez: `ẞ` to `ss`, `ﬁx` to `fix`, `ΣΑΣ` to `σασ`; `#\SPACE` is `#\space`; `arbace/repr/scheme/casefold.ae` and si's `unicode.ae`, generated by `bin/repr-scheme ucd gen`); the `x` of `#A` is not folded (`#\X41` stays an error); a port keeps the directive between reads (si's runtime) |
| `:hash-bang` | any other `#!<identifier>` but `#!r6rs` | `#!chezscheme` is taken and changes nothing (since SCS12 (c); the reading stays R6RS's, so an extension after it, `#!chezscheme \|a\|`, is still a violation: Chez reads its own syntax there and si's runtime its R7RS one, classed `:chezscheme-extensions`); any other a syntax violation (R6RS mode alone: `#!eof`, `#!bwp`) |
| `:polar` | a polar number `m@a` but for an exact zero angle (`m@0` is `m`, the report's), 11.7.4.1's transcendental functions | since SCS14 (b): an exact zero magnitude is the exact `0` (`0@2`, `#e0@2`); with `#e` any other is no number, a syntax violation (`#e1@2`: no exact equivalent); else `m cos a + i m sin a` in binary64: each part's nearest binary64 taken (an exact `2` is `2.0`), the cosine and the sine of the angle correctly rounded (the binary64 nearest the exact value), each product rounded once (`1@2` is `-0.4161468365471424+0.9092974268256817i`, `#i0@2` is `-0.0+0.0i`, `1@1e500` is `+nan.0+nan.0i`); the report computes the cosine and the sine by integer arithmetic with a proven error bound (`arbace.repr.scheme.report`'s `cr-cos`, `cr-sin`) |

Where Chez reads past R6RS's grammar (SCS18 (b), the user 2026-11-09; `test/scheme/report-chez.edn`'s three rules before), si's
choice is the report, not Chez: both readers refuse a `<naninf>` not in lower case (`+INF.0`, `#i-NaN.0`: 4.2.1 spells `inf.0`
and `nan.0`), a `<mantissa width>` after a radix other than 10 (`#x1|53`: only `<decimal 10>` takes one), and end a prefixed
number's token at a `#` (a `<delimiter>`: `#e1#t` is `1` and `#t`, where Chez keeps the R5RS digit placeholder and refuses the
token); in R7RS's syntax (si's `#!r7rs` and string ports in R7RS mode) and with Chez's own lexical syntax (`SI_CHEZ_LEXICAL`,
the mats) si's runtime reads as before. Each is classed against Chez in `test/scheme/choices.edn` (`:chez-naninf-case`,
`:chez-radix-mantissa-width`, `:chez-hash-after-prefixed-number`, with the 37 probes they cover; cases with `:rule :grammar`, no
choice applying), `report-check` holds those probes to differ from Chez's dumps, and the check judges them by the report.

Left to Chez's dumps: an exact exponent beyond the implementation's limits (no probe has one). `test/scheme/choices.edn`
holds cases of each choice with Chez's and si's runtime reader's readings, recorded by hand. Since SCS12 (c) the folding,
the character names and `#!chezscheme` agree three ways (SCS10's three classes are gone); two classes remain:
`:chezscheme-extensions` (an extension after `#!chezscheme`, above) and `:polar-last-bit`, a polar number's part one unit
in the last place off the correctly rounded value, which the test verifies is no more: repr/scheme's reader takes its
cosine and sine from jrt's `StrictMath` (fdlibm's algorithms) and Chez from its C library (musl's, fdlibm's descendant),
both within one ulp but not correctly rounded everywhere; si's runtime (Go's `math`) agreed with the correctly rounded
values on 2,008 sampled angles. A case where repr/scheme's reader reads otherwise than the report names its class
(`:reader-class`). `bin/repr-scheme check` runs `choices-test` over them, and `report-check` compares the choices' cases
among the probes and files with Chez's dumps (they agree: the probes' polar numbers are `1@2`'s, `#i0@2`'s and
`1@1e500`'s, Racket's `base.sls` has `1.1@2.2`).
