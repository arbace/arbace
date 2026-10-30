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
`#;` and the datum after it. So are the directives `#!r6rs`, `#!fold-case` and `#!no-fold-case`, which act on
reading (`#!fold-case` folds the case of identifiers; `#!r6rs` selects the mode, 6). `[` `]` read as the list
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

The expander (SM4) produces, and the evaluator (SM5) and the AOT (SM7) consume, forms of the core language: the
output of Chez's expander (`Lsrc`) with R6RS's names. They are forms like any other; this section only fixes the
shapes, so that examples and tests can be written now.

| core form | shape |
|---|---|
| constant | a self-evaluating datum, or `(quote d)` |
| reference | a symbol |
| `lambda` | `(lambda FORMALS body ...)`, FORMALS a symbol, a list of symbols or `(scm/dot (a b) rest)` |
| `case-lambda` | `(case-lambda (FORMALS body ...) ...)` |
| `if` | `(if test then)` or `(if test then else)` |
| `set!` | `(set! id expr)` |
| `begin` | `(begin expr ...)` |
| `letrec*` | `(letrec* ((id init) ...) body ...)` |
| application | `(operator operand ...)` |
| primitive | a reference to a symbol of a library (`car`, `+`) |

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
and subsequents are the report's, over Unicode general categories (`Character/getType`), validated against
Chez's by an exhaustive scan of the scalar values (9).

## 7. The printer, `:lines`

`repr/scheme print` writes forms as Scheme text: Chez's `write` of each datum, one datum a line, with no layout of
its own (SM3b may add one). Chez's choices, which the check compares to the byte:

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

## 10. The corpora

`test/scheme/corpora.edn`: Racket's R6RS tests (`racket/r6rs`, `r6rs-test/tests`, Apache-2.0 OR MIT), Larceny's
`test/R6RS` (`.sch`), Chibi Scheme's R7RS tests (BSD-3-Clause; the file is R7RS, which Chez refuses: the corpus
of refusals, and SM8's suite) and go-whim's whimsical (read in place, pinned by tree hash), each file pinned by
SHA-256; fetched by `bin/repr-scheme corpus fetch` into `.tmp/scheme/corpora/`. The probes
(`test/scheme/probes.edn`): SCHEME-SURVEY 2.4's, batteries of tokens (prefixes by bodies of numbers, the report's
character names, delimiters, comments, dots), 3,000 random numbers and exhaustive character classes.
