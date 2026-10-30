# Scheme as a future crop — survey and plan

Status: survey and plan, 2026-10-30, at the user's request (a fifth or later language for the REAP matrix:
read, eval, AOT, print). Nothing here is adopted: no code under `arbace/`, `go/`, `bin/` or `test/` changed,
and no Arbace executable was built or run (GT7's clean run was going: light work only). The decisions are the
user's, as proposals SC1-SC14 (§11); the milestones SM1-SM9 (§10) are named by SC1's neighbour, SC9.

Base: branch `scheme-survey`, from main `68b2bba6`. The probes are a few `chez --script` runs of 10 lines
each (§2.4, §3); the scratch is the worktree's `.tmp/scheme/` (`p.ss`, `q.ss`, `r.ss`, Appendix A). Nothing was
downloaded: the corpora of §7 are named with sources and licenses from memory of the projects, and every
license and size marked "to verify" is verified at the fetch the user decides (SC3).

Words used throughout:

- **Scheme forms**: Scheme programs and data written as Arbace forms (§3), the counterpart of the Go forms
  (doc/go/SPEC.md), the Java forms (doc/classes/SPEC.md), the Haskell forms (doc/haskell/SPEC.md) and the Rust
  forms (doc/rust/SPEC.md).
- **repr/scheme**: the bridge between Scheme text and Scheme forms (`arbace.repr.scheme`, `bin/repr-scheme read`,
  `print`, `roundtrip`, `check`), as repr/clojure and repr/go are.
- **si**: the evaluator of Scheme forms at the REPL, as goi is Go's (§5); the name is SC11's.
- **The reference**: Chez Scheme 10.3.0 (§2), the host's `/usr/bin/chez`.
- **The core language**: what the expander produces and the evaluator and the AOT translator consume (§5.1):
  `quote`, references, `lambda`, `case-lambda`, `if`, `set!`, `begin`, `letrec*`, applications, primitives.

Contents: Summary · 1 What parity means for Scheme · 2 The host and the reference · 3 Scheme forms · 4 repr/scheme:
read and print · 5 eval: si · 6 AOT · 7 The corpora · 8 How it fits the existing pieces · 9 The fifth row ·
10 Plan (SM1-SM9) · 11 Proposals (SC1-SC14) · 12 What the user decides first · 13 Risks · 14 Sources · Appendix A

## Summary

- **Scheme is the cheapest read level of any crop and the dearest eval.** Scheme text is already
  s-expressions, so "Scheme as Scheme forms" is a lexical bridge (a Scheme reader in Arbace that yields Clojure
  data, with a spelling for every datum the Clojure reader cannot say, §3), not a grammar. What costs is
  meaning: the numeric tower, `call/cc` with `dynamic-wind`, proper tail calls, hygienic macros (`syntax-rules`
  and R6RS `syntax-case`), the library system and phases, Unicode strings, bytevectors and ports (§5).
- **The reference is on the host and is R6RS.** `/usr/bin/chez` is Alpine's `chez-scheme` 10.3.0-r2 (Chez Scheme
  `10.3.0`, machine `ta6le`, threaded), whose boot files are 1.1 MB and 2.1 MB (`petite.boot`). It reads and
  runs R6RS programs (`(import (rnrs))`), not R7RS: `(import (scheme base))` is "library not found" and Chez's
  reader rejects R7RS's `#u8(` ("invalid sharp-sign prefix #u"; it spells bytevectors `#vu8(`). go-whim chose
  Chez and R6RS in 2026-10 for the same reasons (typed bytevector access, real threads, the Racket team's
  compiler, in apk's community repository) and wrote its editor core as one R6RS library, 54,257 lines, in
  `/root/go-whim/whimsical`.
- **Recommendation: R6RS (the 2007 report, `(rnrs)` libraries) with Chez as the reference (SC1 (a), SC2 (a)),
  R7RS-small later as a library layer judged by a second reference (SC1's step, SM8).** R6RS is what the reference
  runs, what go-whim's code is, and the larger and stricter language (expander with phases, conditions, records
  with protocols, bytevectors, Unicode, ports); R7RS-small is, in large part, a subset in different spelling.
- **Plan (§10): SM1-SM9, about 44 to 75 working days**, the long pole SM5 (the runtime and evaluator, 12-20) and
  SM4 (the expander, 6-10). Two re-seeds (after SM5, SM7), batched, and the tag's.
- **The hard parts, in order:** (1) the expander (hygiene, phases: vendored psyntax, SC5); (2) `call/cc` that
  re-enters, with `dynamic-wind`, on a Go-hosted runtime without tail calls (SC6: an explicit continuation, not
  Go's stack); (3) the numeric tower and its printing byte for byte (SC7); (4) the conditions system and
  `guard`'s exact semantics; (5) the corpora, which the host does not have (SC3).
- **To decide first (§12):** the standard (SC1), the corpus fetch (SC3), the evaluator's shape (SC4, SC6), the
  numeric tower's scope (SC7) and the order after C23 and net/http (SC13).

## 1. What parity means for Scheme

The crops' rule (doc/FULL-SUPPORT-SURVEY.md, "The binding decisions" and "REAP v1's divergences"; C23's §1),
for Scheme:

| cell | in Arbace (proposed) | corpus | reference | judge |
|---|---|---|---|---|
| **Scheme read** | repr/scheme `read`: Arbace's reader of R6RS lexical syntax | every corpus of §7, valid and invalid | Chez's `read`, dumped by a helper script run on Chez (`tools/schemedump`, the way `tools/godump` is Go's) | the data equal, datum by datum, with numbers compared by exactness and value and by Chez's printing of them; a text Chez rejects is rejected with the same class of error |
| **Scheme eval** | si: expander (§5.1), core evaluator (`go/arbace/scm`), the `(rnrs)` libraries | the run programs and test suites | Chez's run of the originals (status, stdout, and a test suite's own pass counts) | status and stdout byte for byte, as goi's; a suite's per-test results against Chez's |
| **Scheme AOT** | core forms → Go forms over the same runtime, built by `arbace.build.go` | as eval | as eval | as eval, and interpreted against compiled (diffcheck's kind) |
| **Scheme print** | repr/scheme `print`: `:lines` (`write` of the datum) and `:full` (the text) | every corpus | the original text; Chez's `write` of the datum | `:full` byte for byte; `:lines` forms → text → forms equal, and its output equal to Chez's `write` |

As for the crops: on the host (linux/amd64) alone, forms only (no Scheme text typed at the REPL), each level's
results recorded as data, `bin/reap` judging them from the records with no runs; every non-pass in a recorded
class (by design or deferred, named).

What differs from the other crops:

- **The language is its data.** There is no grammar to port, so the read level is small and the print level is
  mostly Chez's `write` rules; but the "forms" of a program and a Scheme *datum* are the same thing, which
  forces a decision on how data the Clojure reader cannot hold are spelled (§3), and the evaluator must tell a
  datum from code by its context, as Scheme does.
- **Two standards.** R6RS and R7RS-small disagree (§2.3), and the host has the first only.
- **Continuations are values.** No crop so far has had re-entrant control; goi's goroutines, panics and
  `defer` cover Go's.
- **The reference compiles with a macro expander the program can call at run time** (`eval`, `environment`,
  `syntax-case` at phase 1): the expander is part of the runtime, not a front end that goes away.

## 2. The host and the reference

### 2.1 Chez on the host

| | |
|---|---|
| binary | `/usr/bin/chez` (also `petite`, `chez-script`), Alpine edge community `chez-scheme-10.3.0-r2` |
| version | `chez --version` prints `10.3.0` |
| machine | `ta6le` (threaded x86_64 Linux), files in `/usr/lib/csv10.3.0/ta6le/`: `petite.boot` 2,136,605 bytes, `scheme.boot` and `chez.boot` 1,155,213, `libkernel.a` 922,032, `scheme.h`, `main.o` |
| revision | `/usr/lib/csv10.3.0/ta6le/revision` holds `git`: the build was from a source tarball without a commit id (apk's `source` is the upstream release tarball); the pin is the apk version, and the checksum of the tarball in Alpine aports' `community/chez-scheme/APKBUILD` (to read at the fetch; not read here, no aports on the host) |
| libc | musl; no cgo anywhere in Arbace, and Chez here is used only as a reference process |
| speed to start | `chez --script` of a 10-line program: 0.14 s wall |

go-whim's survey (`/root/go-whim/doc/SCHEME.md`, `80c49bf5`) measured Chez on this host: first in
ecraven's r7rs-benchmarks, a 93,000-line library compiled at `optimize-level 3` in 57 s and 1.5 GB, the
runtime of a linked executable 38 ms. Chez 10.4.0 and 10.4.1 exist upstream (May 2026); Alpine ships 10.3.0, and
the reference is the host's (SC2).

### 2.2 Sources

- Upstream: https://github.com/cisco/ChezScheme (the Racket team maintains it; Apache-2.0 since Chez 9.5.4 /
  10.0, `LICENSE` in the tree; to verify at the fetch). Tag `v10.3.0`. Its `s/` holds the compiler and expander
  (`s/syntax.ss`: Dybvig's psyntax for Chez; `s/cpnanopass.ss`, the nanopass compiler), `mats/` its test suite,
  `csug/` and `release_notes/` its documentation (the Chez Scheme User's Guide, https://cisco.github.io/ChezScheme/csug/).
- Alpine: `https://gitlab.alpinelinux.org/alpine/aports`, `community/chez-scheme/APKBUILD` (to read).
- The language: R6RS final report, 2007-09-26, https://www.r6rs.org (report and standard libraries, the
  "R6RS Rationale"); R7RS-small, 2013-07-06, https://small.r7rs.org. The reports permit unlimited copying
  (to verify the exact grant at the fetch).

### 2.3 R6RS or R7RS

| | R6RS (2007) | R7RS-small (2013) |
|---|---|---|
| libraries | `(library (name) (export ..) (import ..) body)` with versions and phases, `(rnrs)` and 14 standard libraries | `define-library`, `(scheme base)` and 13 more; no phases |
| macros | `syntax-rules` and `syntax-case`, `datum->syntax`, `identifier-syntax`, phases | `syntax-rules` only (`er`/`sc` macros not standard) |
| conditions | condition types, `raise`, `raise-continuable`, `with-exception-handler`, `guard`, `assert`, `&who`... | `raise`, `raise-continuable`, `with-exception-handler`, `guard`, `error-object?` |
| numbers | exact and inexact, `exact-integer-sqrt`, `div`/`mod`/`div-and-mod` (Euclidean), fixnum and flonum libraries, bitwise | `floor/`, `truncate/` etc., `exact-integer?`; no fx/fl libraries |
| strings, chars | immutable literals, Unicode scalar values, `(rnrs unicode)` | `(scheme char)`, `string-foldcase`; strings mutable but literals constant |
| bytevectors | `(rnrs bytevectors)`: typed reads and writes (`bytevector-u32-native-ref`...), `string->utf8`, endianness | `bytevector-u8-ref`, `bytevector-copy`, `utf8->string` only |
| records | `define-record-type` with parent, protocol, `nongenerative`, `sealed`, `opaque`, procedural and inspection layers | `define-record-type` simple |
| I/O | `(rnrs io ports)`: transcoders, binary and custom ports, `(rnrs io simple)` | `(scheme file)`, `(scheme read)`, `(scheme write)`; `read-u8`, bytevector ports |
| lexical | `#vu8(`, `#'x` `#,x` `#`x` syntax quotation, `[ ]` as parens, `#!r6rs`, `#!fold-case` | `#u8(`, datum labels `#0=`/`#0#`, `|a b|` hex escapes, `#!fold-case`; no `#'` |
| `eval` | `(eval expr env)` with `(environment '(rnrs))` | `(eval expr env)` with `(environment '(scheme base))` |
| `syntax-rules` | ellipsis escapes `(... ...)`; underscore | custom ellipsis, `(... ...)`, `_` |

Which is right: R6RS for the first six milestones (SC1 (a)), because the reference runs it, because it is the
larger language, and because everything in R7RS-small has an R6RS counterpart except a few (datum labels,
custom ellipsis, `define-values`, `case-lambda` is R6RS too, parameters and `make-parameter`, `string-map` over
several strings (R6RS has it), `char-ready?`, `read-string`, `features`, `include`, `cond-expand`). R7RS-small
is then a library layer (`(scheme base)` and the rest, written in Arbace/Scheme on top of `(rnrs)` plus the few
primitive differences), judged by a *second* reference since Chez does not read it: Chibi Scheme
(Alpine community `chibi-scheme` 0.12-r0, 4.0 MiB; go-whim's table), whose R7RS test file is the suite (§7). That
is SM8, and optional (SC1 (b) is both from the start, (c) R7RS alone, which no reference on the host runs).

### 2.4 What the probes showed (`.tmp/scheme/p.ss`)

Run on the host's Chez 10.3.0 under `chez --script`:

```
(import (rnrs))
(write (list #\a #\space #\x41 "a\nb" '|a b| 'ABC '#(1 2) #vu8(1 2) 1+2i +inf.0 -0.0 #e1.5 #x-ff
             `(a ,@'(b c) . d)))
=> (#\a #\space #\A "a\nb" a\x20;b ABC #(1 2) #vu8(1 2) 1+2i +inf.0 -0.0 3/2 -255 (a b c . d))
```

- symbols are case-sensitive (`ABC`), and `write` of `|a b|` is `a\x20;b` (R6RS's hex-escape spelling, not R7RS's
  `|a b|`): the printer must reproduce it;
- `(sqrt -4)` is the exact complex `0+2i`, `(* 99999999999 99999999999)` the bignum 9999999999800000000001,
  `(exact (floor 2.5))` is 2, `1/3` inexact prints `0.3333333333333333`, `(expt 2 100)` exact;
- `(let loop ((i 0)) (if (< i 10000000) (loop (+ i 1)) i))` returns 10000000 (tail calls in a loop of 10^7);
- `(call/cc (lambda (k) (dynamic-wind in (lambda () (k 1)) out)))` runs `in`, then `out`, returns 1 (printed
  `inout1`);
- `#;(hidden)` is skipped; `#u8(` is a read error (above); `(import (scheme base))` is a library-not-found error.

## 3. Scheme forms

Arbace's source is read by a Clojure reader (`read`, not EDN: CLAUDE.md's convention). A Scheme program's
*text* cannot be read by it, but a Scheme program is a sequence of data, and the forms are those data spelled so
that the Clojure reader reads them back to the same datum. So the forms are `.ae` files (or `.edn`-like data) of
Clojure lists, symbols, numbers, strings and a handful of tagged lists in a namespace `scm/`.

### 3.1 Where the dialects differ

What the Clojure reader would do to Scheme text, and the spelling the forms take (the rule for each is a row of
repr/scheme's table, as repr/clojure's rules table gives the dialects' divergences):

| Scheme datum | Clojure reader on the same characters | the form |
|---|---|---|
| `#t` `#f` (and `#true` `#false`) | `#t` is a reader error | `true` `false` |
| `()` | `()` the empty list | `()` (Clojure's empty list; never `nil`: `nil` is no Scheme datum) |
| `(a b c)` and `[a b c]` | list; vector | a list; `[`...`]` read as a list (R6RS) and kept in `:full`'s layer (SC12) |
| `(a . b)`, `(a b . c)` | `.` is a reader error | `(scm/dot (a) b)`, `(scm/dot (a b) c)`: the proper prefix and the tail |
| `'x` `` `x `` `,x` `,@x` | `'x` quote; `` ` `` syntax-quote; `,` is whitespace; `,@` ... | `(quote x)`, `(quasiquote x)`, `(unquote x)`, `(unquote-splicing x)`: plain lists, so nested quasiquote is just nesting and its levels are the evaluator's |
| `#'x` `` #`x `` `#,x` `#,@x` (R6RS) | `#'` is `(var x)`; others errors | `(syntax x)`, `(quasisyntax x)`, `(unsyntax x)`, `(unsyntax-splicing x)` |
| `#;datum` | `#;` reader error (`#_` is Clojure's) | dropped in `:lines`; a `(scm/datum-comment datum)` in `:full`'s layer |
| `#| ... |#` (nested) | error | dropped in `:lines`; a comment of `:full`'s layer |
| `; comment` | same | dropped in `:lines`; kept in `:full` |
| `#(1 2 3)` | `#(` is Clojure's fn literal | a vector `[1 2 3]` |
| `#vu8(1 2)` / `#u8(1 2)` | error | `(scm/bytevector 1 2)` (the spelling is kept in `:full`; Chez reads only `#vu8(`) |
| `#\a` `#\space` `#\x41` `#\nul` | `#\` is an error; Clojure chars are `\a` `\space` `A` | Clojure char `\a` for a character in the BMP that is not a surrogate; else `(scm/char 128512)` (a Scheme character is a Unicode scalar value; Java's `char` is not) |
| `"a\x41;b\n"` | `\x` is an unsupported escape | a string of the same characters; strings beyond the BMP are Java strings of UTF-16 pairs, converted at the evaluator's boundary (SC8's `[]rune`) |
| `|a b|`, `a|b c|d` | `|` is a symbol constituent... or an error | `(scm/sym "a b")` |
| a symbol the Clojure reader would not read back unchanged (`1+`, `-`... `true`, `nil`, `a/b`, `a:`, `.`, `@x`, `:k`, `#a`) | number, namespaced symbol, literal, keyword, error | `(scm/sym "1+")`; rule: **bare when `read` of the printed name is the same symbol, `(scm/sym "...")` otherwise**, tested by the printer, not tabulated |
| symbols: R6RS `...`, `->x`, `+`, `-` | `...` fine, `->x` fine | bare |
| `1/2`, `-3`, `100` | ratio, long, long | same; a number beyond a long is a BigInt (`123N`); `#e1.5` is `3/2` in `:lines` (the datum), `#x-ff` is `-255` |
| `1.5` `1e3` `-0.0` | double | same; `+inf.0` `-inf.0` `+nan.0` | `(scm/flonum +inf.0)` spelled `(scm/flonum "+inf.0")`; `-0.0` is Clojure `-0.0` |
| `1+2i`, `+i`, `1@2` (exact or inexact complex) | error | `(scm/complex re im)` with parts as above, `(scm/complex 1 2)` exact, `(scm/complex 1.0 2.0)` inexact; polar forms are read to rectangular in `:lines` and kept in `:full` |
| `#e1.1` `#i1/3` `#b101` | error | the value, with the spelling kept in `:full` (R6RS has no mixed-exactness complex) |
| `#!r6rs`, `#!eof`, `#!default`, `#!fold-case`, `#!no-fold-case` | error | `(scm/directive "r6rs")` ... |
| datum labels `#0=(a . #0#)` (R7RS) | error | `(scm/label 0 datum)` and `(scm/ref 0)`, resolved by the evaluator; cyclic data are never forms |
| `#!eof` object, chars that are control or non-characters | -- | `(scm/directive "eof")`, `(scm/char n)` |

### 3.2 What a program is

Top-level R6RS programs are `(import ...)` then body forms; libraries are `(library (name) (export ...) (import
...) body ...)`. These are data like any other: the forms of a file are a vector of top-level data (as the
other bridges' module forms are), and nothing is wrapped. `define-record-type`, `define-syntax`, `syntax-rules`,
`call/cc`, `dynamic-wind` are ordinary symbols in ordinary lists: there is no node kind for them in the forms,
because their meaning is the expander's and the evaluator's (§5), exactly as in Scheme.

The consequence for Arbace: a Scheme program is never evaluated by Arbace's Clojure evaluator by accident. The
forms of a file are *data*; the entry `(scm/eval forms env)` (SC11) gives them meaning. Scheme symbols are
Clojure symbols with no namespace (a symbol `a/b` is `(scm/sym "a/b")`).

### 3.3 Modes

As repr/go and repr/java: `:lines` keeps the datum and its line (positions as metadata or a side table, SC12);
`:full` keeps everything needed to print the text back byte for byte: comments, whitespace and layout, spellings
of numbers and characters and symbols' case and escapes, `[ ]` against `( )`, the datum comments. The mechanism
(a layer beside the datum, keyed by position) is the Go forms' and the Clojure bridge's `.cljc` layer's.

## 4. repr/scheme: read and print

### 4.1 Read

A reader of R6RS's lexical syntax (chapter 4 of the report and its appendix on the lexical grammar) in Arbace,
~600 lines: delimiters, identifiers (initials, subsequents, peculiar identifiers, `\x41;` escapes), numbers
(`<prefix> <complex>` with radix, exactness, `/`, decimals, exponent marks `e s f d l` and `|` mantissa widths
`1.5|53`, `+inf.0 -inf.0 +nan.0`, complex rectangular and polar), characters (names `nul alarm backspace tab
linefeed newline vtab page return esc space delete`, `#\xHEX`, `#\U+...` not R6RS), strings with line
continuations (`\<newline><intraline whitespace>`), `#vu8(`, vectors, nested block comments, datum comments,
`#!fold-case`. Case-sensitive. The reader runs on the tools' host, evaluated from the checkout (no re-seed) until
SM3 compiles it.

The oracle is a script `tools/schemedump/dump.ss` run by Chez (`chez --script`), reading a file with Chez's own
`read` (one datum at a time, with `(rnrs io ports)` positions through `get-datum` / `read` on a source-file
port: Chez gives annotations by `(read-token)` / `annotation` via `chezscheme`'s `read-with-source-annotations`
or `(source-file-descriptor ...)`; which to use is verified at SM1) and printing each datum in a tagged line
format the Arbace side reads (one tag per datum kind, numbers as exactness plus Chez's `number->string` plus the
value in a normal form). The check is E-tree (data equal), as repr/go's native-check.

What is hard: numbers (the grammar is the largest part of R6RS's lexical syntax; Chez has its own accepted
extensions, e.g. `#e1.5` fractions, `1e500` to `+inf.0`, `#x1.8`? classed at SM2); symbol case folding under
`#!fold-case`; surrogate and non-character `\x` escapes (errors in Chez, must be refused the same); and the
error side (Chez's message for a bad token is `Exception in read: ...`; the check compares the *class* and the
line, not the words, a class in the recorded divergences).

### 4.2 Print

`write` and `display` of a datum, as Chez prints them (the `write` rules of R6RS §8.2: strings with `\n`,
`\"`, `\\`, `\xHEX;` for non-graphic characters; symbols with `\x..;` for characters outside the identifier
syntax; characters `#\a #\space #\x7f?`) — **Chez's, not Arbace's Clojure printer's**: `a\x20;b` for `|a b|`
(§2.4), `#vu8(1 2)`, `1e21` and `1.5` flonum text by shortest round-trip with Chez's choices (`1e21` not
`1.0E21`; `+inf.0`; the leading zero rules `0.5`), exact rationals `3/2`, complex `1+2i`. Cyclic data and
shared structure: `write` of a cycle is an infinite loop in R6RS (Chez's `write` is safe? it is not by default;
`print-graph` parameter): by design out, class recorded.

`:lines` prints the forms back as Scheme text in a canonical layout (one top-level datum a line group,
indentation by Scheme's convention as a person writes it — optional layout printer, SM3b); `:full` prints
the original text.

### 4.3 Judging

`bin/repr-scheme roundtrip`: text → forms → text byte for byte (`:full`); text → forms → text → forms equal
(`:lines`); and forms → Chez's `write` against `print`'s `write` (the data print level). The corpora of §7.

## 5. eval: si

### 5.1 The parts

Following goi's design (doc/go/GOI-SPEC.md) and the C23 survey's ci:

1. **The expander** `arbace.scheme.expand`: from forms to the core language, with R6RS's semantics: libraries with
   imports/exports/versions and renaming, phases (`(for (meta 1) ...)`), `syntax-rules`, `syntax-case`,
   `datum->syntax`, `identifier-syntax`, `define-record-type` / `define-enumeration` as the report defines them
   (derived forms), `let-values`, `do`, `case`, `guard`, `parameterize`? (R6RS has no `parameterize`; Chez does;
   R7RS does). The core language is the output of Chez's expander (`s/syntax.ss`'s `Lsrc`: about ten
   forms).
2. **The runtime** `go/arbace/scm` (Go forms, hand-written, in goi's table like `go/arbace/goi`): values (below),
   the numeric tower (§5.4), strings and characters, symbols, pairs and vectors, bytevectors, hashtables (eq, eqv,
   equal, string, symbol, generic with user hash), records, conditions, ports, `dynamic-wind`, continuations,
   parameters, `eval`, `environment`.
3. **The evaluator**: the core language compiled to a closure tree and run by a loop with an explicit
   continuation (§5.2).
4. **The builder and REPL** `arbace.scheme.build`, `arbace.scheme.repl`, `bin/si run|check|test|divergences` (as
   `bin/goi`).

Values in the Go runtime: fixnums `int64` within Chez's 61-bit range (`fixnum?`, `(fixnum-width)` = 61 on
`ta6le`), bignums `*big.Int`, ratnums `*big.Rat`, flonums `float64`, exact and inexact complex (§5.4); booleans
`bool`; characters `rune`-valued `Char`; strings `*String{[]rune, mutable}` (R6RS strings are mutable and
random-access by scalar value; Java/Go UTF-16 or UTF-8 strings are not); symbols interned `*Symbol`; pairs `*Pair`
(cons cells: lists are not Clojure lists at runtime; the form-to-value conversion is the evaluator's `quote`);
the empty list a singleton; vectors `[]Value`; bytevectors `[]byte`; procedures `*Closure`, `*Primitive`, `*Cont`,
`*Param`; the unspecified value; the eof object; record instances `*Record`; conditions as records.

### 5.2 Tail calls and call/cc

Go has neither tail calls nor first-class continuations. A direct-style closure tree on Go's stack gets one by
trampoline (tail calls) and escapes only by panic/recover. Full Scheme needs re-entry: generators, coroutines,
`call/cc`-based backtracking (`amb`), exceptions that resume (`raise-continuable` handlers run in the dynamic
environment of the raise, and `guard`'s reraise re-enters it), `dynamic-wind` in both directions, and
unbounded non-tail recursion (a non-tail-recursive `(define (sum n) (if (= n 0) 0 (+ n (sum (- n 1)))))` with
`n` = 10^6 must run; Go's default stack limit is 1 GB, so it would, but at 100+ bytes a Go frame it is a
liability).

Three designs:

- **(a) An explicit continuation, heap-allocated frames** (a CEK / register machine; the frames are a linked
  list). `call/cc` captures the pointer (O(1), re-entrant any number of times, multi-shot); tail calls are free
  (the continuation is not extended); recursion depth is bounded by memory, not by Go's stack; `dynamic-wind` is
  a wind list carried with the continuation and unwound by comparison on a switch, exactly as the report's
  reference implementation does; `with-exception-handler` is a handler stack in the dynamic environment. Cost:
  every non-tail call allocates a frame (3 to 10 times slower than direct style on call-heavy code; the
  interpreter is the slow path anyway, and AOT, §6, recovers part).
- **(b) Direct style, tail calls by trampoline, `call/cc` escape-only (one-shot, upward) by panic, re-entry by
  goroutine hand-off for one-shot generators.** Fast, most real code works (early exit, exceptions, `guard`), but
  multi-shot and re-entering continuations do not exist, `dynamic-wind` re-entry cannot be done, and the R6RS
  suites test them (§7.5): every such test would be a class "deferred forever".
- **(c) Direct style plus copying Go's stack**: impossible in Go.

Recommendation: **(a) for si**, because the test suites exercise re-entry and the cost buys the semantics the
reference gives; and for AOT a CPS translation over the same frames (§6), SC6.

Proper tail calls in (a) cover `if`, `cond`, `case`, `and`, `or`, `when`, `unless`, `begin`, `do` bodies, `let`
forms and applications in tail position, as the report requires; checked by the suites' tail-call tests and the
10^7 loop above.

### 5.3 Conditions

R6RS conditions are record-based: simple conditions (`&message`, `&irritants`, `&who`, `&assertion`,
`&violation`, `&error`, `&warning`, `&serious`, `&syntax`, `&lexical`, `&non-continuable`, `&i/o-*`, `&undefined`),
compound conditions, `condition-predicate`, `condition-accessor`. `raise` makes a non-continuable raise (a handler
that returns triggers `&non-continuable` as a secondary raise), `raise-continuable` returns the handler's value,
`with-exception-handler` installs a handler for the dynamic extent *of the thunk* with the handler running in
the context of the raise minus itself, `guard` is `call/cc` plus `dynamic-wind` in its reference expansion (the
semantics: the clauses are tried in the dynamic environment of the `guard`, a reraise returns to that of the
`raise`). The primitives' errors must raise the same condition types Chez does (`(car 5)` raises `&assertion`
with `&who` `car`, `&message`, `&irritants`). Uncaught: Chez prints `Exception in car: 5 is not a pair` and exits
1; the judge compares the status and stdout, and the stderr text only by its first token and the `Exception in`
prefix (a class).

### 5.4 The numeric tower

R6RS requires exact integers, exact rationals and inexact reals (flonums); complex numbers are optional but Chez
has exact and inexact complex numbers, and the suites test them (`make-rectangular`, `sqrt` of a negative,
`exp`, `log`, `expt` with complex bases, `angle`, `magnitude`). Parts:

- integers: `int64` fixnum plus `*big.Int`, with Chez's 61-bit fixnum boundary visible to `fixnum?`,
  `greatest-fixnum`, `fx+` overflow errors and `fx+/wraparound` (Chez, used by go-whim);
- rationals: `*big.Rat`, always normalized; `numerator`, `denominator`, `rationalize`;
- flonums: `float64`, with Chez's printing (§4.2), `flonum->fixnum`, `fl+`..., NaN payloads not;
- `exact`/`inexact`, `exact-integer-sqrt`, `div`/`mod`/`div0`/`mod0` (R6RS's Euclidean variants),
  `floor/` and `truncate/` (R7RS), `number->string` and `string->number` with radix and precision;
- exact transcendental behaviour as Chez: `(exp 0)` is exact 1, `(sqrt 4)` exact 2, `(expt 2 -2)` is 1/4,
  `(sqrt -4)` is exact `0+2i`, `(atan 1 0)`, `(exact (expt 2.0 100))`; many are "exactness-preserving when the
  result is exact" rules to take from Chez's behaviour, probed one by one;
- complex: exact complex as a pair of `big.Rat`, inexact as `complex128`, with `exp`, `log`, `sin`..., `sqrt`
  branch cuts as Chez (and Go's `math/cmplx` is not guaranteed to agree: classed per function).

Go's `math/big` covers the exact side; the inexact printing is Arbace's own (shortest round-trip digits from
`strconv.FormatFloat(x, 'e', -1, 64)` re-shaped to Chez's format, a few lines).

### 5.5 Unicode, bytevectors, ports, hashtables, sorting

`(rnrs unicode)`: `char-upcase` and friends, `string-upcase` with special casing (`ß` → `SS`), `string-foldcase`,
`string-normalize-nfd/nfc/nfkd/nfkc`, `char-general-category`; Chez 10.3.0 implements Unicode 15?; Go 1.27.1's
`unicode` is a newer table: differences land in classes. Normalization needs `golang.org/x/text/unicode/norm`
(not in std; vendored as Go forms if wanted — an SC item, deferred by design is the alternative). `(rnrs
bytevectors)`: all `bytevector-*-ref/set!` with endianness (`native` = little on amd64), `bytevector->uint-list`,
IEEE `bytevector-ieee-single-ref`, `string->utf8/16/32`. `(rnrs io ports)`: file and string ports, binary and
textual, transcoders (UTF-8/16/Latin-1, eol styles, error handling modes), custom ports; `(rnrs files)`;
`(rnrs programs)`: `command-line`, `exit`. `(rnrs hashtables)`: `make-eq-hashtable`, `equal-hash`, etc.; the
iteration order of a hashtable is unspecified and differs from Chez's (tests that print it are classed). `(rnrs
sorting)`: `list-sort`, `vector-sort!`; stability is not required but Chez's is stable (merge sort); copy it.

## 6. AOT

Two routes, as C23's §7 offers:

- **Core forms → Go forms**, printed by repr/go, built by `arbace.build.go` over the same `go/arbace/scm`
  runtime: each lambda a Go function over frames; non-tail calls become explicit continuation frames (a CPS
  translation: the continuation a struct holding a Go closure), tail calls and self tail calls a loop (`for`), known primitives
  inlined (fixnum `+` with overflow fallback). Recommended (SC9 (a)).
- **Scheme → Arbace forms (Clojure) → c2j → j2g**: the existing AOT. Rejected on the facts of §5.2: Arbace's
  Clojure has `recur` and no tail calls, no first-class continuations; a Scheme-to-Clojure translation would have to
  contain the same CPS and a trampoline in Clojure, run through c2j, j2g and Go; slower and one language too
  many. It is only considered for the expander if the expander is written in Arbace (SC5 (b)).

What is hard: CPS code size and speed in Go (every call site a closure allocation; the interpreter's design cost
reappears, and the AOT is a constant-factor win, 2-5x), `call/cc` and `dynamic-wind` exactly as in si (the same
frames, so the same semantics: diffcheck's kind of check between the interpreted and compiled runs), `eval` at
run time in a compiled program (the expander and the evaluator linked in, as Chez's `scheme.boot` is), and
libraries compiled separately (a compiled library is a Go package with its exports and visit/invoke code, phase
by phase). The expander itself, written in Scheme, is the AOT's first customer (§8).

What the reference judges: the same programs' status and stdout from Chez's own `compile-file` path is not
needed; Chez interpreting or compiling, the output is the same, and the check is the corpora run by si and by
the compiled build against recorded Chez runs (`--record-ref` as `bin/java-check` records the JDK's).

## 7. The corpora

None of these is on the host (searched: no `*r7rs*`, `*r6rs*` files; Chez ships its `mats/` only in the source
tarball, not in the apk). Named with source, license and an estimate, all to verify at the fetch (SC3); sizes
are from memory of the projects and flagged "about".

| corpus | what | source | license | size |
|---|---|---|---|---|
| R6RS conformance (Larceny) | `test/R6RS/` : tests of the base library, lists, records, syntax-case, arithmetic, bytevectors, etc., in R6RS programs | https://github.com/larcenists/larceny, `test/R6RS` | Larceny's own BSD-style license (to verify) | about 20 files |
| R6RS tests (Racket's `r6rs`) | Matthew Flatt's R6RS test collection, run on any R6RS implementation | https://github.com/racket/r6rs (`tests/r6rs`) | MIT / Apache-2.0 (to verify) | about 10,000 lines |
| Chez's mats | `mats/*.ms`, the implementation's own regression suite: mostly Chez-specific (internals, `$` primitives), with a standard-conforming part (arithmetic, strings, records, conditions, hashtables, syntax) | https://github.com/cisco/ChezScheme `mats/` (run by `mats/Mf-base`) | Apache-2.0 | tens of files, hundreds of thousands of checks (to count); needs the `mat` framework (a macro, `mats/mat.ss`) |
| R7RS (Chibi's) | `tests/r7rs-tests.scm`: one file covering R7RS-small, with `(test expected expr)` macros | https://github.com/ashinn/chibi-scheme | BSD-3-Clause | about 1,500 tests; one file |
| R7RS coverage | `r7rs-coverage` : tests of every R7RS binding | https://github.com/ecraven/r7rs-coverage (to verify) | to verify | one file |
| SRFI reference implementations | SRFI 1 (lists), 13, 14, 26, 39, 69, 78, 98, 125, 128, 133, 158 and their sample tests | https://srfi.schemers.org (each in its own repository) | MIT, per SRFI | tens of files, about 10,000 lines |
| ecraven's r7rs-benchmarks | the benchmark programs (`nqueens`, `tak`, `fib`, `ack`, `nboyer`...) for speed and output | https://github.com/ecraven/r7rs-benchmarks | MIT (to verify) | about 50 programs |
| R5RS/R6RS standard examples | the report's own examples, extracted | the reports' texts | the reports' grant | about 500 |
| go-whim's whimsical | the editor core, generated, as one R6RS library plus runtime libraries | `/root/go-whim/whimsical/whimsical/{editor,rt,host,term,printf}.ss`, `main.ss` at `80c49bf5` | go-whim's `LICENSE` is the Vim license (the code is derived from Vim); stated in LICENSE.md if vendored | 52,645 + 506 + 158 + 311 + 637 = 54,257 lines |

Notes on each:

- **R6RS suites are the right judges.** The Racket/Larceny R6RS tests check an implementation's `(rnrs)` against
  the report and run on Chez; each run prints failures. The check records per-test results (the suite's
  test names are the ids, as the Clojure suite's), against the same suite's run on Chez as the reference, so a
  test Chez itself fails is the reference's, not Arbace's.
- **Chez's mats** are the reference's own tests and most of them are not portable (internals). The portable
  subset is chosen by running them *on Arbace's si* and classing: Chez-specific names (`$...`, `(chezscheme)`
  forms) are "by design". Their volume is the largest and their value is highest for the numeric tower and
  `number->string` byte for byte.
- **go-whim's whimsical** is a stress corpus of the dialect actually in use. Read/print (all five files) is
  within SM2-SM3. Eval needs `(chezscheme)`'s `foreign-procedure` (libc calls through `write(2)`), `fork-thread`,
  `$`-primitives: 70 uses of `$` in `editor.ss` alone, `fxsll/wraparound` and `fx*/wraparound` in `rt.ss` (counts
  by `grep`): by design out, except the library loads and a subset of the `testdata` instance runs once a thin
  `(chezscheme)` subset exists (SC10). Its R6RS `bytevector-u32-native-ref` core is exactly the typed-bytevector
  workload of §5.5.
- **A corpus of invalid programs** (syntax errors, unbound identifiers, `&syntax` conditions raised at expand
  time) is built from the suites' `assert-syntax-violation` checks and Chez's mats' error tests.

## 8. How it fits the existing pieces

- **repr/clojure's rules table** (doc/REPR-CLOJURE.md): the same shape for the lexical rows of §3.1, one table
  of rules `{:scheme-spelling ... :form ...}`, each reversed by `print`, with the one-way rules documented
  (datum comments dropped in `:lines`, `#e`/radix collapsed to the value). repr/scheme is its sibling bridge,
  not a variant: Clojure is read by the Clojure reader; Scheme needs its own.
- **repr/go, repr/haskell, repr/rust**: the structure to copy (spec `doc/scheme/SPEC.md`, SM1; helper
  `tools/schemedump`; `bin/repr-scheme`; modes; the round trip's levels). Unlike them there is no grammar helper
  in a host compiler's API but a plain script on Chez; unlike repr/haskell and repr/rust, this crop needs no
  host compiler's *frontend* after the oracle is recorded, since the reader is small enough to have only a
  recorded reference (the data of a corpus read by Chez), so the check needs Chez only to record (as C's gcc).
- **goi** (doc/go/GOI-SPEC.md) is the model of si: an Arbace-side checker/builder, a Go back end hand-written as
  Go forms in goi's table, `bin/si run|check|test|divergences`, the divergences file in `test/goi/divergences.edn`'s
  format, `bin/reap` judging from records. The expander is the analogue of goi's checker.
- **c2j and j2g** are the AOT for Arbace's own namespaces; Scheme's AOT does not go through them (§6). They matter
  in one place: if the expander is written in Scheme (SC5 (a)), it is compiled by the AOT of this crop, and if in
  Arbace (SC5 (b)), by c2j like any namespace.
- **go-whim's `crefactor/togo`** (a Scheme backend; `--scheme`, the generated R6RS of `whimsical`) is a *producer* of
  Scheme, not a reader or evaluator: it shows what an R6RS program looks like at scale and that `(rnrs)` plus a thin
  `(whimsical rt)` of Chez extras was enough for 55k lines. It is a corpus (§7), not prior art for si.
- **The Haskell/Rust parked crops** (repr/haskell, repr/rust) show the cost model: a bridge with a helper on the
  host's compiler took 6 to 10 milestones; this crop's reader is cheaper, its evaluator dearer.
- **Re-seeds**: the tools' reader and expander run evaluated from the checkout (like repr/haskell); the runtime
  `go/arbace/scm` is part of the program, so SM5 changes the executable (a re-seed), SM7's AOT changes the
  program again (batched), SM9 re-seeds once more for the tag. SC14.

## 9. The fifth row

If C23 lands first (`arbace-reaps-4-crops-v1`, CS2), Scheme is the fifth row:

| cell | sources | classes |
|---|---|---|
| `[:scheme :read]` | `bin/repr-scheme check` (the data against Chez's dumps, the errors' classes): `test/scheme/check-read.edn` | `test/scheme/divergences-read.edn` |
| `[:scheme :eval]` | `bin/si check`: `test/scheme/check-linux-amd64.edn` | `test/scheme/divergences.edn` |
| `[:scheme :aot]` | `bin/si check --aot`: `test/scheme/check-aot-linux-amd64.edn` | `test/scheme/divergences-aot.edn` |
| `[:scheme :print]` | `bin/repr-scheme roundtrip` (`:full` byte for byte, forms again, `write` against Chez's): `test/scheme/roundtrip-known.edn` | `test/scheme/divergences-print.edn` |

`test/reap/cells.edn` gains `:scheme` in `:languages`; until a check is recorded its cell prints "no check yet"
with `:until` the milestone. The essential gate gains the essential subsets at SM9 (seconds each). The tag:
`arbace-reaps-5-crops-v1` (after C23), annotated with R6RS (2007), Chez 10.3.0 and the corpora's pins.

## 10. Plan (SM1-SM9)

Estimates in working days. Each milestone ends with its check green and its records (journal, agenda, this
survey's "As built"). "S" names the cheaper model, "O" the harder.

| milestone | content | exit criteria | days | model |
|---|---|---|---:|---|
| **SM1** the spec | `doc/scheme/SPEC.md`: Scheme forms (§3) complete for R6RS lexical syntax and the core language, worked examples; `tools/schemedump/dump.ss` (the oracle on Chez) ; the corpora fetched or the fetch refused (SC3), `test/scheme/corpora.edn` with pins; the probes of §2.4 as tests | the spec accepted by the user; the manifest lists every corpus with its pin and license in LICENSE.md | 2-3 | O: the data model; S: the manifest, the dump script, license entries |
| **SM2** repr/scheme read and print `:lines` | the reader (R6RS lexical), the printer (Chez's `write`), `bin/repr-scheme read|print|check`; E-tree against Chez's dumps | every corpus read, equal to Chez's reading; invalid programs refused as classes; `write` equal to Chez's over the corpora's data | 3-5 | S: lexer pieces, tables; O: numbers and their printing |
| **SM3** `:full` and the layout printer | the layer (comments, spellings), `roundtrip` byte for byte; the reader listed in goi's table and compiled (one re-seed batched with SM5) | `:full` byte for byte over every corpus; whimsical's five files in | 2-4 | S |
| **SM4** the expander | libraries with phases, `syntax-rules`, `syntax-case`, `datum->syntax`, derived forms, `eval`/`environment`; the core language; SC5's route | the suites' syntax tests and `assert-syntax-violation` checks, `&syntax` conditions with the right forms; the expander's own tests (the report's macro examples, Dybvig's hygiene cases) | 6-10 | O: the expander; S: derived forms |
| **SM5** the runtime and si | `go/arbace/scm` (values, numbers §5.4, strings, symbols, records, conditions, hashtables, ports basic), the explicit-continuation evaluator (§5.2), `dynamic-wind`, `call/cc`, `with-exception-handler`, `guard`, `bin/si run|check|test|divergences`, Chez's runs recorded (`--record-ref`) | `bin/si check` over the R6RS suites' run programs: none failing, every non-pass in a class; the 10^7 loop and 10^6 deep recursion; generators and `amb` through `call/cc` | 12-20 | O: continuations, numbers, conditions; S: primitives, tables |
| **SM6** the other libraries | `(rnrs bytevectors)`, `(rnrs unicode)`, full `(rnrs io ports)` with transcoders, `(rnrs files)`, `(rnrs programs)`, `(rnrs enums)`, `(rnrs sorting)`, `(rnrs arithmetic fixnums/flonums/bitwise)`, `(rnrs records *)`, `(rnrs mutable-*)`, SRFI 1/13/14/26/39/69/98/125/128/133/158 as libraries | each `(rnrs ...)` library's tests in the suites in a result or a class; the SRFI samples pass | 8-14 | S: nearly all; O: Unicode normalization, ports |
| **SM7** AOT | core forms → Go forms over `go/arbace/scm`; `bin/si check --aot` (shards built by `arbace.build.go`, under the lock); the expander compiled and linked (so `eval` works in a compiled program); libraries as packages | every run program compiled or `refused` with a reason; none failing; interpreted and compiled results compared | 6-10 | O: CPS translation; S: harness |
| **SM8** corpora and R7RS | Chez's mats (portable subset classed), the SRFI tests, ecraven's benchmarks, the R7RS layer `(scheme base)` and the rest (SC1's step, judged on Chibi's suite with Chibi as the second reference) | every file of the corpora in a result or a class; R7RS: the suite's file run, per-test results against Chibi's | 3-6 | S |
| **SM9** the fifth row | `cells.edn`'s `:scheme`, classes files, `bin/reap` with five rows, the essential subsets in `bin/gate`, `make` targets (`repr-scheme`, `si-check`) and `make check-makefile`; the milestone run, the fixpoint, the re-seed, the tag | `bin/reap`: 20 of 20 cells checked, none failing, no class pending; the tag (SC13) | 2-3 | S: row and targets; O: the divergences |

**Total: about 44 to 75 working days** (2-3, 3-5, 2-4, 6-10, 12-20, 8-14, 6-10, 3-6, 2-3). SM1-SM3 change only tools'
namespaces (evaluated or interpreted until a re-seed); SM5 and SM7 change the program: **two re-seeds**, one
after SM5 (the runtime and the compiled reader and expander) and one after SM7 (the AOT), and SM9's own for the
tag; the SM3 re-seed is folded into SM5's. SM6 can run beside SM5's second half once the primitives' interface is fixed; SM8 beside SM7.

Which tasks suit Sonnet (as much as possible, per the standing rule): the manifest, the dump script, license
entries, lexer pieces and tables, derived forms of the expander (`do`, `case`, `let-values`, ...), most
primitives and libraries of SM6, the check harness, the divergences files, `cells.edn`, Makefile targets, the SRFI
ports. Opus for: the data model's spec (SM1), numbers and their printing (SM2, SM5), the expander (SM4: psyntax's
marks, wraps, phases), the continuation machinery and conditions (SM5), the CPS translation (SM7), Unicode
normalization and the transcoders (SM6), and the classing of divergences.

## 11. Proposals (SC1-SC14)

Each with its options, the recommended one first.

- **SC1 The standard.**
  - Options: (a) R6RS (2007, `(rnrs)` and the 14 standard libraries) first, R7RS-small later as a library layer
    judged by Chibi (SM8); (b) both from the start, R7RS with Chibi as a second reference from SM2; (c) R7RS-small
    alone (no reference on the host runs it: Chez reads neither `#u8(` nor `(scheme base)`).
  - *Recommended: (a)*: the reference runs R6RS, go-whim's code is R6RS, and R6RS is the larger language, so R7RS-small
    after it is mostly spelling and a second reference.
- **SC2 The reference implementation.**
  - Options: (a) the host's Chez 10.3.0 (apk `chez-scheme-10.3.0-r2`), its runs recorded as data so checks need
    no Chez; (b) Chez 10.4.1 from upstream, built (needs a C compiler, `make`; not on the standalone path); (c)
    Racket CS or Guile (Alpine has both, not as reference-grade for R6RS).
  - *Recommended: (a)*: it is on the host, pinned by the apk version, and the same record-as-data rule as gcc and
    the JDK.
- **SC3 The corpus.**
  - Options: (a) fetch and pin Racket's `r6rs` tests (or Larceny's `test/R6RS`), Chibi's `r7rs-tests.scm`, Chez's
    `mats` (tarball for `v10.3.0`), SRFI reference implementations and ecraven's benchmarks, each pinned by SHA-256,
    licenses in LICENSE.md; (b) fetch only the two R6RS suites and Chibi's file, add the rest later; (c) no
    fetch: hand-write the corpus from the report's examples (about 500) and go-whim's five files.
  - *Recommended: (a)* in stages, (b) first: a language is judged by suites, and the two R6RS suites are small.
    Which of Racket's or Larceny's tests: decided at the fetch, with a count; the fetch itself is the user's
    decision (as C23's CS15).
- **SC4 Is eval an interpreter or a translation to Arbace forms?**
  - Options: (a) an interpreter in Go forms (`go/arbace/scm`), a closure tree over explicit continuation frames,
    fed by an expander in Arbace or Scheme (goi's design); (b) an interpreter written in Arbace (Clojure),
    compiled by c2j (slower, no new Go package, less to re-seed); (c) a translation of Scheme to Arbace's
    Clojure forms evaluated by Arbace (no tail calls, no continuations: needs the CPS and a trampoline anyway).
  - *Recommended: (a)*: continuations and tail calls need the machine to be the program's own, the runtime's data
    (strings as runes, tower numbers) belongs in Go, and goi is the working pattern. (b) as a prototype in SM1 is
    cheap and tells the cost.
- **SC5 The expander.**
  - Options: (a) vendor psyntax (Dybvig/Ghuloum's portable expander; Chez's `s/syntax.ss` is Apache-2.0 but uses
    Chez internals; the portable `psyntax.ss` of Ikarus/Larceny/Ypsilon is permissively licensed in some forms, GPL in
    Ikarus's: the license to verify) as Scheme forms through repr/scheme, run by si, then AOT-compiled and linked;
    (b) write a `syntax-rules`/`syntax-case` expander in Arbace (hygiene by renaming with marks, phases: the
    biggest single item, 6-10 days); (c) `syntax-rules` only non-hygienic first (R7RS-level), `syntax-case` later.
  - *Recommended: (a) for the destination, with (c) as SM4's first week* so SM5 can start: psyntax is the
    reference design, proves the self-hosting claim (Scheme's own expander running on Arbace) and vendors by the
    pattern Arbace likes (prior art in, record origin, license in LICENSE.md). Risk in §13.
- **SC6 call/cc.**
  - Options: (a) explicit heap continuation frames (CEK), re-entrant and multi-shot, `dynamic-wind` by wind list;
    (b) direct style with escape-only `call/cc` by panic/recover and one-shot re-entry by goroutines; (c) CPS
    for all code at AOT only (interpreter stays (b)).
  - *Recommended: (a)* for si and a CPS AOT over the same frames (§6): the suites test re-entry and the choice
    must be made once; (b) as a labelled fallback if (a)'s cost (3-10x) fails the CPU limits of `bin/lib/limit.bash`
    on the corpora.
- **SC7 The numeric tower.**
  - Options: (a) real tower from SM5 (integers, rationals, flonums, `math/big`), complex numbers deferred as a
    recorded class until SM6 (exact complex as `Rat` pairs, inexact `complex128`); (b) the full tower including
    complex at SM5; (c) integers and flonums only, rationals and complex by design out.
  - *Recommended: (a)*: rationals are R6RS's base and cheap (`big.Rat`); complex is optional in the report, Chez has
    it, the suites test it lightly, and its branch-cut agreement with Chez is open work (§13).
- **SC8 Strings and characters.**
  - Options: (a) runtime strings as `[]rune` (mutable, O(1) `string-ref` by scalar value), characters Unicode
    scalar values, forms carry strings as Java strings and `(scm/char n)` above the BMP; (b) Go strings (UTF-8): O(n)
    `string-ref`; (c) Java/UTF-16 strings as Arbace's runtime has: wrong for `string-length` of non-BMP text.
  - *Recommended: (a)*: R6RS says so.
- **SC9 Milestones' names and the AOT route.**
  - Options: (a) milestones SM1-SM9 (S1-S9 and SC are used elsewhere), AOT as core → Go forms with CPS (§6);
    (b) SCH1-SCH9; (c) AOT through c2j.
  - *Recommended: (a)*: the names are unused; (c) is rejected in §6.
- **SC10 The `(chezscheme)` subset.**
  - Options: (a) a thin `(chezscheme)` library with what real code uses (`fx+/wraparound` and its relatives, `printf`/`format`,
    `fork-thread` and mutexes as goroutines, `eval` with `interaction-environment`, `sort`, `list-head`, `iota`,
    `box`, `time`, `parameterize`/`make-parameter`, `define-structure`, `case-lambda`, `meta-cond`), no `$` internals and no
    `foreign-procedure`; (b) none: only `(rnrs)` and SRFIs; (c) a large `(chezscheme)` including FFI.
  - *Recommended: (a)* after SM6, so whimsical's libraries load (and its runtime library `(whimsical rt)` shows the real need); the FFI
    is by design out.
- **SC11 Names.**
  - Options: (a) the interpreter `si` (`bin/si`), the bridge `repr/scheme` (`arbace.repr.scheme`, `bin/repr-scheme`),
    the Go runtime `go/arbace/scm`, the namespace prefix `arbace.scheme.*`, form tags `scm/`; (b) `schemei`, `repr/chez`
    (wrong: Chez is the reference, not the language); (c) `ssi`.
  - *Recommended: (a)*.
- **SC12 Read details.**
  - Options: (a) positions as a side table (line, column, end) keyed by datum identity, `[ ]` and comments in `:full`'s
    layer, as repr/go's `:full`; (b) positions as metadata on each list; (c) no positions in `:lines`.
  - *Recommended: (a)*: metadata would put positions into `=` semantics for lists and the eval test's `equal?`
    comparisons.
- **SC13 The order relative to C23 and net/http.**
  - Options: (a) after C23 (C3b-C9 are in the agenda), and net/http's work (as the user names it; no survey on main) before or beside as
    the agenda orders, with SM1-SM2 (spec, reader and printer, 5-8 days, tools only, no re-seed) as a light item that
    can run beside any heavy milestone, SM3-SM9 after C23's tag; (b) before C23: Scheme's read level is the cheaper
    and does not depend on C's cc port; (c) after everything.
  - *Recommended: (a)*: C23 is decided and under way and shares the same new machinery (a fifth-row template,
    re-seed batching); SM1-SM2 are the part that fits the gate's light windows; net/http, being Go
    library work on goi's side, goes by the agenda's order, not Scheme's.
- **SC14 Re-seeds.**
  - Options: (a) two re-seeds (after SM5 and SM7) plus the tag's, tools evaluated from the checkout until then; (b)
    one re-seed after SM7, SM5-SM6 evaluated; (c) a re-seed per milestone.
  - *Recommended: (a)*: SM5's runtime is the first change to the program; evaluating it would hide the speed.

## 12. What the user decides first

Before SM1 starts:

1. **SC1**, the standard (R6RS first).
2. **SC3**, whether the suites may be fetched, and which: Racket's `r6rs` tests or Larceny's `test/R6RS`, Chibi's
   file, the Chez tarball.
3. **SC4 and SC6**, the evaluator's shape and its continuation machinery (the two decisions the cost depends on).
4. **SC5**, the expander (vendored psyntax or written).
5. **SC13**, the order after C23 and net/http.

The rest (SC2, SC7-SC12, SC14) can be taken as recommended and revisited when the spec (SM1) is reviewed.

## 13. Risks

- **The expander is the largest single risk**: psyntax is subtle (marks, wraps, substitutions, phases, a library
  system that visits and invokes in dependency order), its Chez internals are not portable, and its licenses are
  mixed across forks. Fallback: SC5 (c), `syntax-rules` alone, which is what R7RS needs and what most corpora use,
  with `syntax-case` classed deferred.
- **Re-entrant continuations cost speed**: (a)'s frames may make the corpora miss the CPU limits; the interpreter's
  design is fixed before AOT, so a mistake here is paid twice. Mitigation: prototype in Arbace at SM1 (SC4 (b)).
- **Printing numbers byte for byte** (flonums, rationals, complex, radix) is the finest-grained judge and the
  reference's choices are not always the report's; budget in SM2/SM5.
- **The corpora are not on the host**: SC3's fetch is blocking for a *judged* crop (without it the crop checks
  against the report's examples and whimsical alone); licenses of Larceny's and Racket's suites and of
  `psyntax` variants are from memory.
- **Chez-specific code**: whimsical and Chez's mats use `(chezscheme)` and internals; the divergence classes
  will be many and honest ("by design: `$` primitives, FFI").
- **Unicode versions**: Chez 10.3.0's tables against Go 1.27.1's `unicode`; `string-upcase`, `char-general-category`
  and normalization differences are classed, normalization needs a vendored `x/text/unicode/norm`.
- **Hashtables' iteration order, `eq?` on numbers and characters, string literals' mutability, evaluation order of
  arguments**: unspecified in R6RS and Chez picks (right to left for applications in some compiled cases); the
  reference's choice leaks into outputs. Judged on status and stdout of UB-free programs; a second run through
  Chez with `--optimize-level`/`interpret` finds the leaks, as C23's `-O2` run does.
- **Program growth and startup**: the runtime, the expander and the libraries linked into the executable
  (psyntax compiled is large; Chez's own boot files are 1.1-2.1 MB) — measured at SM5 and SM7, as GP7's was.
- **R7RS is not the host's language**: the second reference (Chibi) is an apk package that is not installed; its
  runs would be recorded, once, with the standalone rule (needs an `apk add` for recording only).

## 14. Sources

- go-whim, `https://github.com/arbace/go-whim` at `80c49bf53cd6340487ac623f3d6d135b77ac1db6`, checked out at
  `/root/go-whim` (read, not modified): `doc/SCHEME.md` (the choice of Chez and R6RS, the table of candidates, the
  measurements), `doc/SCHEME-IDIOMS.md`, `whimsical/` (`main.ss`, `whimsical/{editor,rt,host,term,printf}.ss`),
  `crefactor/togo` (the Scheme backend). Grep counts in §7 were taken on these files.
- Chez Scheme: `https://github.com/cisco/ChezScheme` (v10.3.0; Apache-2.0, to verify), the CSUG
  `https://cisco.github.io/ChezScheme/csug/`, release notes `https://cisco.github.io/ChezScheme/release_notes/v10.0/release_notes.html`;
  the host's `/usr/bin/chez` (apk `chez-scheme-10.3.0-r2`, `/usr/lib/csv10.3.0/ta6le/`).
- The reports: R6RS `https://www.r6rs.org` (2007-09-26) and R7RS-small `https://small.r7rs.org` (2013-07-06); Dybvig,
  Hieb, Bruggeman, "Syntactic abstraction in Scheme" (1992), and Dybvig's psyntax; Clinger, Rees et al. R5RS
  (1998).
- Named, not fetched: Larceny `https://github.com/larcenists/larceny` (`test/R6RS`), Racket's `r6rs`
  `https://github.com/racket/r6rs`, Chibi Scheme `https://github.com/ashinn/chibi-scheme` (`tests/r7rs-tests.scm`,
  Alpine `chibi-scheme` 0.12-r0), ecraven's `r7rs-benchmarks` and `r7rs-coverage`
  (`https://github.com/ecraven/`), the SRFI process `https://srfi.schemers.org`, Ikarus, Ypsilon, Loko Scheme
  (`https://scheme.fail`) as other R6RS implementations, Alpine aports `community/chez-scheme/APKBUILD`.
- Arbace (main `68b2bba6`): CLAUDE.md, doc/FULL-SUPPORT-SURVEY.md, doc/C23-SURVEY.md (structure, §1, §7, §11, §12),
  doc/haskell/SPEC.md and doc/rust/SPEC.md (the repr pattern), doc/go/GOI-SPEC.md, doc/REPR-CLOJURE.md, doc/AGENDA.md,
  test/reap/cells.edn, doc/go/BOX-SURVEY.md.

## Appendix A: the scratch

`.tmp/scheme/` of the worktree `.tmp/worktrees/scheme-survey`: `p.ss` (§2.4: `write` of a list of data, the tower,
`call/cc` with `dynamic-wind`, the 10^7 tail loop), `q.ss` (a `call/cc` escape), `r.ss` (`(import (scheme base))`:
"library (scheme base) not found"). Run as `chez --script FILE`; the whole took under a second.
