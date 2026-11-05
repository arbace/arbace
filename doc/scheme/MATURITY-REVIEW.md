# Scheme maturity review (2026-11-05)

Branch `scheme-maturity`, from `scheme-sm9` (`4ff315063`). A light review: no gate, no build of Arbace, no corpus run. Measured from the
committed records and classes (`test/scheme/check-*.edn`, `divergences*.edn`, counted by script over the records' items) and by reading
si's implementation (`go/arbace/scm/`, `scheme/lib/`), plus about 150 probe expressions run on the light driver (`bin/si light`, 10 s;
`bin/si run`; main's stage 0 as host). Counts of "tests regained" below are estimates from the records, marked as such.

## 1. Where si stands

si is an R6RS system (psyntax, the full numeric tower, conditions, libraries) with R7RS-small as libraries over it (SM8). Against R7RS-small
itself it is nearly complete; what separates it from "mature" is (a) speed, (b) Chez's own extensions, which Chez's mats exercise, and
(c) a few corners found below.

| corpus (record) | what runs | non-pass | in classes |
|---|---|---|---|
| Chibi's R7RS suite (`check-r7rs`) | 1,225 tests | 9 | all `r7rs-float-printing` (deferred) |
| Chibi's own library tests | 268 | 1 | `chibi-optional-fallback` (a test-suite bug) |
| SRFI programs (`check-srfi`) | 11 SRFIs, 1,231 checks | 0 | |
| si's check (`check-linux-amd64`) | 47 programs (Racket's and Larceny's R6RS suites, si's own) | 1 | `chez-conditions` (design) |
| AOT (`check-aot`) | 45 programs | 3 | `chez-conditions` 1, `syntax-constants` 2 (deferred) |
| ecraven's benchmarks | 58 | 4 | `ecraven-too-slow` 3, `ecraven-reader-corners` 1 (INCORRECT everywhere) |
| Chez's mats (`check-mats`) | 911 mats: 300 pass, 89 fail, 511 not run, 11 skipped; clauses run alone: 4,335 pass, 283 fail, 8,266 in mats that do not run | 1,468 items in 23 classes | |

The mats are Chez's own tests (Chez extensions, compiler passes, foreign calls), not R7RS: they measure parity with Chez, not with the report.

### The mats' 1,468 items by class

| class | items | kind | what it is |
|---|---|---|---|
| `mats-chez-identifier` | 506 (268 distinct identifiers) | design | mat not run: an identifier of Chez's own; the top ones are compiler tests (`test-cp0-expansion` 64, `equivalent-expansion?` 26, `optimize-level` 11, `enable-cp0` 9, `cfl~=` 11), the rest a long tail (`native->signed` 9, `load` 8, `machine-type` 6, `define-ftype` 6, `sinh`..`atanh` 2 each, `immutable-string?`, `fluid-let-syntax`, `make-record-type`, ...) |
| `mats-chez-reader` | 228 | design | mat not run: Chez's lexical syntax (`#vfx(` 76 uses in 6 files, `#vfl(` 74, `#!eof` 35, `#!bwp` 31); 140 of the 228 are "unexpected close parenthesis" or "end-of-file" after the first bad read: a cascade |
| `mats-numeric` | 186 | deferred | 5_3 `string->number` of Chez's extended syntax (about 85 clauses), `expt`, `div-and-mod`, `/`, fl/ieee corners, `r6rs:number->string` with a precision |
| `mats-error-checks` | 126 | design | Chez raises on a circular or improper list given to `memp find assp andmap ormap exists for-all fold-left for-each`, `iota`, `list-copy`; si loops (watchdog) or returns |
| `mats-returned-false` | 94 | deferred | clauses after a clause that did not run; `call/1cc` identity |
| `mats-format` | 91 | deferred | Chez's `format` directives with parameters |
| `mats-library-missing` 55, `mats-chez-checks` 33, `mats-chez-syntax` 28, `mats-files-missing` 28, `mats-arity-one` 26 | 170 | | Chez's compiled-library files, cp0, `(char=? c)`, ... |
| the other 11 classes | 93 | | `mats-chezscheme-subset` 17, `mats-clause-errors` 12, `mats-parameters` 11, `mats-timing` 5, ... |

## 2. The features, probed

Each row: what si does, from the probe programs (`.tmp/p/a.sps`, `b.sps`, run on the light driver) and the code.

| area | status | evidence |
|---|---|---|
| Exact rationals, bignums, exact/inexact | complete | `(/ 1 3)` 1/3, `(expt 2 200)`, `(exact 0.1)` 3602879701896397/36028797018963968, `(exact 1e300)` exact, `(round 5/2)` 2, `exact-integer-sqrt` of 10^41; fixnum = Chez's 61 bits |
| Complex | present | `(sqrt -4)` 0+2i, `(* 2+3i 4-5i)`, `make-polar`, `magnitude` (complex.ae; the header of number.ae is stale) |
| `number->string` | shortest digits, radix 2 to 36 | `(number->string 0.1 2)`, `1e100`, `1.2345678901234567e19`; **denormals print Chez's R6RS mantissa-width suffix: `5e-324|1`** (the 9 Chibi failures); the R6RS precision argument is not accepted (`mats-numeric`) |
| `string->number` | R7RS and R6RS syntax | `#e1.5`, `1/2`, `+inf.0`, `#xFF`, prefix order; Chez's extended syntax is the `mats-numeric` tail |
| `call/cc` | **full, re-entrant, multi-shot**: the continuation is the list of heap frames (O(1) capture) with the wind list and handler stack | generator by re-entry gives (1 2 3 done); re-entry with `dynamic-wind` runs `<><>`; one restriction: re-entry into a continuation captured inside a Go callback (sort's comparison, hashtable hash and equivalence, custom-port procedures, record protocols) is an `&implementation-restriction` (`eval.ae:193`) |
| Tail calls | proper in every position tried | `and`/`or`/`when`/`cond`/`case`/`do`, mutual recursion, 3,000,000-iteration loops; non-tail recursion to 3,000,000 deep runs (heap frames); `delay-force` stream of 10^6 in constant space |
| `dynamic-wind`, `guard`, `raise`, `raise-continuable`, `with-exception-handler` | correct | reraise from `guard` without a matching clause, handler return from `raise` ("handler returned from non-continuable") |
| Ports and I/O | string, bytevector, file, custom (SC180) ports; pushback, `read-line`, `read-string`, `read-u8`, `peek-u8`, `read-bytevector`, closed-port errors, `file-error?`, `read-error?` | all probes pass; `call-with-output-string` is not in R7RS (not provided) |
| Strings, chars, Unicode | full Unicode (`full-unicode` in `features`) | `(string-upcase "straße")` "STRASSE", final sigma in `string-downcase`, `string-ci=?` "Straße" "STRASSE", `digit-value` of Arabic-Indic digits, astral characters |
| `syntax-rules` | R7RS's (SM8b) | nested ellipsis, custom ellipsis (`:::`), literals shadowed by a binding, tail patterns after an ellipsis, vector patterns, `_`, hygiene (`my-or` with `t`) |
| `define-record-type`, `parameterize` (with converter), `case` with `=>`, `let*-values`, `delay-force`, `make-promise`, `string-map`/`vector-map` of unequal lengths, `vector-copy!`/`bytevector-copy!` overlapping, datum labels (`read`, `write`, `write-shared`) | correct | probes |
| Libraries | all 16 R7RS-small libraries, `define-library` (`export import begin cond-expand include include-ci include-library-declarations`, read by the host) | `scheme/lib/scheme/`; `cond-expand` with `library`, `features` |
| **`include`, `include-ci` as expressions** | **were missing** (`unbound identifier include`) | built here, SMR1 (section 5) |
| `eval`, environments | `(eval e (environment ...))` works | **`(eval '(define x 3) (interaction-environment))` is refused** ("a definition was found where an expression was expected"; R6RS environments are immutable); so is a `define` in any `eval` environment |
| SRFIs | the 11 vendored (1, 13, 14, 26, 39, 69, 98, 125, 128, 133, 158) all pass, 1,231 checks | `grep` of the corpora's imports: no other SRFI is imported by a program si runs (112, 64, 19, 116, 126 occur only in other implementations' preludes of ecraven's tree and in SRFI 125's and 128's own tests' conditionals) |
| Speed | interpreter 44 times Chez (median over 24 benchmarks of the ecraven record, 140 at most: `lattice`); AOT about 2 times the interpreter (fib 40: 41 s, 84 s, Chez 0.85 s) | `ecraven-too-slow` 3 at 300 s (`earley`, `takl`, `ntakl`), 12 names over the 60 s limit |

## 3. Gaps ranked by tests regained per day

Estimates; "regained" means items leaving a class (pass, or moving to a smaller class). Days are working days.

| rank | gap | items | days | per day | note |
|---|---|---|---|---|---|
| 1 | SMR2 list-argument checks (circular and improper lists) in the machine's higher-order primitives | `mats-error-checks` 126 | 2 | about 60 | design class today; the primitives are already step machines (`prim_control.ae`) |
| 2 | SMR3 Chez's lexical extensions for the mats' reader | up to 228 (140 cascade) | 2 to 3 | about 40, bounded: a mat read still has to run | R7RS untouched; a reader mode of the harness only |
| 3 | SMR4 numeric corners | `mats-numeric` 186 | 4 | about 35 | `string->number` extended syntax, precision argument, flonum `remainder`/`modulo`/`min`/`max`, `expt`, `div-and-mod` |
| 4 | SMR5 denormal printing in R7RS mode | 9 Chibi tests | 0.5 | 18 | decided class SC22; needs the user |
| 5 | SMR6 the Chez-identifier tail | 506 mats not run, 8,266 clauses | 2 for 40 identifiers | 10 to 20 mats, about 100 clauses | each identifier unlocks a mat only when it is its last obstacle |
| 6 | `mats-format` | 91 | 3 to 4 | about 20 | Chez's `format`; not R7RS |
| 7 | speed (SMR7) | `ecraven-too-slow` 3, then 9 more at the 60 s limit | 5 to 8 | 1 | the maturity item that matters to users; not a test count |
| 8 | `eval` with `define` in the interaction environment (SMR8) | 0 in corpora | 1 | | R7RS REPL conformance |
| 9 | re-entry into a Go callback | 0 known (`call/1cc` identity 4) | 3 | | rewrite sort as a step machine; hashtables' callbacks are the hard part |
| done | `include`, `include-ci` (SMR1) | 0 in corpora | 0.2 | | built |

Not ranked: `syntax-constants` (AOT, 2 programs: psyntax's syntax objects as constants), `chez-conditions` (design), the reader corners of read0
(INCORRECT on every implementation).

## 4. Designs of the top five

**SMR2 list-argument checks.** Chez's `for-all`, `exists`, `andmap`, `ormap`, `map`, `for-each`, `fold-left`, `memp`, `find`, `assp`,
`list-copy`, `iota` raise `&assertion` when the list argument is improper or circular (Chez's tortoise and hare in its "list-length" checks), and
`iota` checks its count. In `go/arbace/scm/prim_control.ae` each step primitive receives its lists once: add a `checkedLen(name, l)` (Floyd's
cycle test over `*Pair`, O(n), once at entry) before the first step, raising the existing assertion condition with Chez's message class. Cost: one
pass over the list per call (the checks Chez makes too); measure on `ecraven` `nboyer` and `sboyer` (list-heavy) before the record. The clauses of
`mats-error-checks` are named by file, mat and clause, so a regained clause makes its line stale in the regex of `divergences-corpora.edn`: edit the
class as the review-009 note says. Check: `bin/si mats 4.ms 5_2.ms` (the two files with most of the 126), `bin/si unit`, `bin/si check --only essential-check.txt`.

**SMR3 Chez's lexical extensions for the mats.** The harness' reader (`arbace.scheme.corpora`, the mats' run) reads each mat form by form and stops at
the first bad form. Add to the reader's mode for the mats `#vfx(`, `#vfl(` (fixnum and flonum vectors as plain vectors of those values, `fxvector?`
and `flvector?` primitives needing the types: either real new vector kinds in `value.ae` or vectors tagged by a wrapper), `#!eof`, `#!bwp` (the
eof object and the broken-weak-pointer object), and make a failed read resynchronise at the next top-level `(mat ` instead of cascading: that
alone removes about 140 of the 228 items' noise. Order: resync first (0.5 day, measure the new items' classes), then the four syntaxes. The read
level (`bin/repr-scheme check`, Chez as oracle) keeps its R6RS lexical mode; the new syntax is a harness option, named in the class text.

**SMR4 numeric corners.** Work from the 186 items by the mat names in the class regex: (1) `string->number` of Chez's extended syntax (5_3 clauses 17..297,
about 85 of them: `#e1@1`, exponent markers `s f d l`, `|` precision, radix with exponent, trailing `.`, signs on `nan`/`inf` with `@`); (2) `number->string`'s
R6RS precision argument `(number->string z radix precision)`, a gap of `(rnrs)` not of Chez's extensions (3 clauses); (3) flonum corners: `flmin`/`flmax`
with NaN, `fldiv-and-mod`, `fl+` with signed zeros, `remainder`/`modulo` on flonums, ieee.ms `expt`, `sqrt`, `magnitude`; (4) `bitwise-rotate-bit-field` with a bignum
count. Parts are separate Go functions in `prim_number.ae` (2,088 lines) and `numtext.ae`: do (1) first with a table-driven test in `numbers_cases*_test.ae`
(the unit tests exist and cost no gate), compare each answer with Chez through the probes (`chez --script`).

**SMR5 denormal printing in R7RS mode.** Today `(number->string 5e-324)` is `"5e-324|1"` (`numtext.ae:77`): the R6RS suffix, which R7RS's grammar does not have
(a `string->number` of it works only because si's reader accepts R6RS). The suite's accepted strings are `"5.0e-324"`, `"1.0e-323"`, `"2.0e-323"`, `"2.5e-323"`,
`"2.4e-322"`, `"2.421e-320"`: the shortest digits, a `.0` when one digit, no suffix. Option (a): in R7RS mode drop the suffix and write `d.0e-N` for a one-digit
denormal mantissa (the 9 tests, 0.5 day, si differs from Chez in that mode, and the Chez reference run of the suite keeps failing the same 9 with its own
text); (b) drop the suffix only (0 tests regained, R7RS-readable output); (c) keep (SC22 as decided). Needs the mode flag in the runtime (today a library-level
choice, not a Go-level one): a parameter set by `(scheme base)`'s import is not possible; the flag would be the `--r7rs` of `bin/si run`.

**SMR6 the Chez identifier tail.** Count per identifier the mats it blocks as their *last* obstacle: rerun the mats with each of the candidate identifiers
bound to a stub raising "not implemented" and see which mats then expand (one `bin/si mats FILE` per file, the files of `fx`, `bytevector`, `misc`, `8`, `primvars`,
`hash`, `5_6` first: 72 + 62 + 46 + 45 + 24 + 19 + 18 items). Implement in `scheme/lib/chezscheme.sls` and `prim_*.ae` the ones that make a mat expand:
the simple ones are `native->signed`, `native->unsigned`, `little-endian->signed`, `bytevector->s8-list`, `sinh`..`atanh`, `immutable-string?`,
`vector->immutable-vector`, `machine-type`, `threaded?`, `top-level-bound?`, `statistics`. The compiler ones (`test-cp0-expansion`, `equivalent-expansion?`,
`optimize-level`, `enable-cp0`, `cfl~=`, `$suppress-primitive-inlining`: 114 mats of `cp0.ms`, `cptypes.ms`, `cfl.ms`) are out of scope: si has no cp0 (a
class by design to keep).

**SMR7 speed (not a count).** The AOT's next step is its own survey option (b) (SCHEME-SURVEY.md, "direct entry": a compiled caller runs a compiled callee's body
directly, a depth counter bounding the Go stack and falling back to frames when it is exceeded) plus unboxed fixnum arithmetic on `int64` when both operands
are fixnum; target fib 40 under 10 s. 5 to 8 days; measured by `bin/si bench` and `bin/si ecraven --only` on the slow list.

## 5. Built here: SMR1, `include` and `include-ci` (0.2 day)

`scheme/lib/scheme/base.sls` now exports `include` and `include-ci`, two `syntax-case` transformers (phase-1 imports of `(only (rnrs) ...)`, the
`except` list of the phase-0 import not shared by it) that read the named files at expansion (`get-string-all`, `read`; `include-ci` prefixes `#!fold-case`)
and splice the data as a `begin`; several file names are taken, definitions land at the position of the form (also in a body). The name is taken as
given (the process's directory); `define-library`'s own include declarations stay the host's (`system.ae`, relative to the library file).

Checks run (light, main's stage 0 as host): `test/scheme/si/include/include.sps` (with `inc.scm`, `inc2.scm`, `ci.scm`, `expected.txt`, run from its directory)
prints `(42 14 5)`, `8`, `missing` (a missing file raises a catchable condition, also through `eval`); `bin/si r7rs` 1,216 of 1,225, regressions: none; `bin/si srfi` 11
programs, 1,231 checks pass, regressions: none; `bin/si test --si` 61 of 64, 3 known, 0 failing. Not done: `base.chezscheme.sls` (the library
Chez runs for the reference) does not carry it, so a program using `include` is not differentially checked until it does and the program is added to
`test/scheme/si/programs.edn` with a Chez run (`bin/si check --record-ref`); the gate's essential steps were not run (no gate by day).

## Proposed (SMR1 onward; none decided)

- **SMR1 include.** (a) keep the built `include`/`include-ci`, add it to `base.chezscheme.sls` and the include sample to `programs.edn` with a Chez run, at the next
  night; (b) as built, no differential program. *Recommended: (a)*.
- **SMR2 the list-argument checks** (rank 1). (a) do it, 2 days; (b) leave `mats-error-checks` a design class. *Recommended: (a)*: it is also R6RS's `&assertion` behaviour.
- **SMR3 the mats' reader** (rank 2). (a) resynchronise first, then the four syntaxes, 2 to 3 days; (b) resynchronise only (0.5 day); (c) none. *Recommended: (b) then (a) on what it shows*.
- **SMR4 numeric corners** (rank 3). (a) all four parts, 4 days; (b) `string->number` and the precision argument only, 2 days. *Recommended: (b), then the rest*.
- **SMR5 denormal printing** (rank 4). (a) R7RS mode only, `d.0e-N`, no suffix; (b) no suffix; (c) keep SC22. *Recommended: (b)*: R7RS-readable output without leaving Chez's digits; (a) only if the nine tests are worth a mode-specific rule.
- **SMR6 the Chez identifier tail.** (a) the stub experiment first (half a day), then the simple identifiers; (b) leave the class. *Recommended: (a)*.
- **SMR7 speed.** (a) direct entry and fixnum fast paths (5 to 8 days) before the Scheme tag; (b) after the tag with the AOT's heavy window; (c) leave. *Recommended: (b)*.
- **SMR8 mutable interaction environment.** (a) `eval` of `define` into `(interaction-environment)` (a top-level record of the machine, 1 day); (b) leave: R7RS programs do not need it. *Recommended: (b) until a corpus needs it*.
