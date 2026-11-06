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

## 6. As built: SMR2 and SMR3 (2026-11-05, as decided: SMR2 (a), SMR3 (b) then (a))

**SMR2, the list-argument checks** (`go/arbace/scm/prim_control.ae`, `prim_list.ae`, `prim.ae`, `scheme/lib/chezscheme.sls`). Most of what the design
listed was there already (the machine's `map`, `for-all`, `exists`, `fold-left`, `fold-right` checked their lists' shape through `checkLists`; the
cyclic and improper clauses of `for-all` and `exists` passed). What the 88 failing clauses of `4.ms` and `5_2.ms` were: (1) `andmap` and `ormap`
were Scheme definitions in `(chezscheme)` with no checks: now `andmap` is `for-all` and `ormap` is `exists`; (2) the lists Chez calls *altered during
the operation*: `for-each`, `for-all`, `exists` (and so `andmap`, `ormap`) count the length found at the entry (`checkLists` returns it) and walk
exactly that many steps through `carsCdrsN`, a tail that is no pair before the count is out raises Chez's "input list was altered during operation"
(a list that grew is ignored, as Chez's); `map` and `fold-right` do not look (Chez's do not: probed), `fold-left` raises on a tail that is neither a
pair nor the empty list (`carsCdrsAlt`), a shortened list ends it quietly, as Chez's; `(chezscheme)`'s `for-each` (it returns its last call's value) has the
same count in its one- and two-list loops; (3) `memp`, `find`, `assp` (`findStep`) now carry the original list, a tortoise and a step count: improper
list raises "improper list ~s" ("improperly formed alist ~s" for `assp`, also for an element that is no pair), circular "cyclic list ~s" ("cyclic
alist ~s"), the words of Chez's; `dlMem` and `dlAss` (`memq`, `assq` ...) say "cyclic list ~s" and "cyclic alist ~s" now too; (4) `ArgList` says "~s is circular" for a
circular list, as Chez does; (5) `iota` is Chez's, a nonnegative fixnum count only (`(iota 3 17)` raises: `(srfi :1)`'s keeps its start and step), and `list-copy`
is `(append l '())`, which checks the list. Probes against Chez (`chez --script`, the same expression on both, 84 calls of the list procedures on
circular, improper, mismatched, altered and non-procedure arguments, compared for raising or not): the differences at the start were 24, at the end one:
a write of a circular result (which si's non-R7RS `write` does not label, a printer matter). `test/scheme/si/programs/list-checks.sps` (40 calls, prints
`ok` or `raise`) is in `programs.edn` with its Chez run (`bin/si check --record-ref`), in the essential subset, passing as interpreted and as compiled
(`bin/si aot`, output equal to Chez's).

**SMR3, the mats' reader.** The review's guess of the cascade was wrong in its cause. `test/scheme/mats/run.sps` cuts the text into top-level data
by counting parentheses (`segments`, `datum-end`), and it took `#;` (the datum comment) for `#` and a `;` line comment, so `#;(error? ...)` ate the
rest of its line and left the parentheses of the next line unbalanced: `hash.ms` alone had 76 reads that failed for that. Built: (a) `#;` skips the
datum that follows (top level and inside a datum); (b) resynchronisation: the harness reads datum by datum from a position, and a datum the reader
refuses is skipped to the next line that begins with `(` in its first column, not to the end of the segment (the old precomputed segments are gone);
(c) the four syntaxes: `#!eof` and `#!bwp` (new singleton `Bwp`, `value.ae`) as data and `#vfx(`, `#vfl(` as vectors (an element that is no fixnum or no flonum is
a read error), in `go/arbace/scm/read.ae` under the flag `ChezLex`, off, turned on by the environment variable `SI_CHEZ_LEXICAL` (set, not `""` or `0`), which `bin/si mats`
sets for the harness: no primitive, so psyntax's boot list and `psyntax.pp.ae` are untouched, and no reader of R7RS, R6RS or of a program changes. The tests:
`TestReadChezLexical` (`reader_test.ae`). Not done: `#N` before `vu8`, `vfx`, `vfl` and `(` (a counted vector, 9 uses in `6.ms`, `bytevector.ms`),
`#!base-rtd`, binary floating-point literals, `#&`, `#{...}`, the names `rubout`, `bel`, `377`; no `fxvector` and `flvector` types (a literal reads as a vector, so a mat
that asks `fxvector?` still stops at an identifier or fails), no weak pairs (`#!bwp` is read, `bwp-object?` is not there).

Read errors after the change, all 68 files of the mats at the reader's level (the harness' own reading, no run): 228 items before, 76 after (record.ms and 7.ms
newly appear in the 76: 12 and 3, files not in the record), the cause by count: `#!base-rtd` 6, binary floating literals 8, counted `#N...(` 15, `#&` 5, `#{` 14, the
names 8, the rest single.

**What moved in the mats' classes, on the mats run** (`bin/si mats --record 3.ms 4.ms 5_1.ms 5_2.ms hash.ms`, 171 mats, light, main's stage 0 as host; items before
(the record at its commit) and after):

| class | before | after |
|---|---|---|
| `mats-chez-reader` | 117 | 7 |
| `mats-error-checks` | 95 | 21 (88 of the 4.ms and 5_2.ms items regained but 2; 12 new in `3.ms`: letrec's restriction, clauses of mats the cascade hid) |
| `mats-chez-identifier` | 50 | 70 (mats that read now meet an identifier of Chez's own: 14 more mats in `hash.ms`, 3 in `3.ms`, 2 in `5_1.ms`, 1 in `4.ms`) |
| `mats-returned-false` | 21 | 25 (`hash.ms` `$symbol-hashtable` 17, 23, 28, 39, which read what a clause that did not run made) |
| other classes | 124 | 124 (unchanged) |
| mats passing in the five files | 70 | 81 |

`4.ms`: pass 12 to 17, fail 7 to 2; `5_2.ms`: pass 25 to 30, fail 20 to 15; `hash.ms`: 20 mats to 34 (none run: all stop at an identifier); `3.ms`: 14 to 17.
The classes' regexes in `test/scheme/divergences-corpora.edn` follow (the regained clauses out of `mats-error-checks`, the newly seen ones in); the record
`test/scheme/check-mats-linux-amd64.edn` is rewritten for these five files only (the partial record of SC182 (b)), the other files' lines are the
old record's until the night's run. The clauses of `mats-error-checks` in the files not run here (`8.ms`, `5_7.ms`, `fx.ms`, `3.ms`'s older ones) are unchanged
by this work, except any that used the lists above: the night's whole run shows it.

Checks run (light, `ARBACE_HOST_EXE` main's stage 0, no gate): `bin/si r7rs` 1,216 of 1,225, 9 float-printing, regressions: none; `bin/si srfi` 11 programs, 1,231
checks pass, regressions: none; `bin/si test --si` 61 of 64, 3 known, 0 failing; `bin/si unit` ok; `bin/si check --only test/scheme/essential-check.txt` 47 of 48
(the one known, `chez-conditions`), regressions: none. Not measured: the speed on `ecraven`'s `nboyer` and `sboyer` (the added cost is one counter in
`for-each`, `for-all`, `exists`, one cycle step in `memp`, `find`, `assp`, no new pass over the list; measure with the night's `bin/si ecraven --only`).

**The night's command** (the whole mats run, heavy, a record of every file; main's lock as every heavy run): `bin/heavy-lock bin/si mats --record`, then
`bin/si check --aot --only si/list-checks` for the AOT record line (here added by hand after a `bin/si aot` run equal to Chez's); the classes
then show what the unrun files moved (`mats-chez-reader` and `mats-error-checks` first).

## 7. As built: SMR4 and SMR5 (2026-11-05, as decided: SMR4 all parts, SMR5 in R7RS mode)

**Two string->numbers.** Probed against Chez (`chez --script` and `chez --program`): Chez's own `string->number` and `number->string` read and write more than
`(rnrs)`'s (a Chez program run as `--program` with `(import (rnrs))` gets the strict ones: `#x1.8`, `1#`, `#12r10`, `#b1e1`, `1/2e3` are `#f`, radix 3 or 36 an
error), and the mats test both (`string->number` and `r6rs:string->number`). si's primitives (`go/arbace/scm/prim_number.ae`) stay R6RS's, and `(chezscheme)`
(`scheme/lib/chezscheme.sls`) defines its own `string->number` and `number->string` over them with one mark in the place (rnrs)'s do not take (a third argument
`#t` for `string->number`, a fourth for `number->string`; any other value there is an error, as the arity is in Chez's `(rnrs)`): no new primitive, so psyntax's boot
list and `psyntax.pp.ae` are untouched. The mark is `StringToNumberX`'s `ext` (`numtext.ae`).

**SMR4 (1), the extended syntax** (`numtext.ae`: `StringToNumberX`, `parseGeneral`, a fallback after the R6RS grammar fails, so every number that read before reads
the same): prefixes `#e #i #x #b #o #d` in any order and `#Nr` (radix 2 to 36, with `#e`/`#i`), a radix argument 2 to 36; digits then `#` placeholders (`98##`, `2.###d3`,
`21##.####`, `1#/5##`) which make the number inexact unless `#e`; a point in any radix (`#x1.8` is 1.5); an exponent marker `e s f d l` that is no digit of the radix, its digits
and sign in the radix and the radix the base (`#b1e10` is 4.0, `1.0e10` in radix 12 is 12^12); a rational with an exponent (`1/5S4`, `#e2/3e4`) and with `#`; `1/0#`, `0/0e20`
(`+inf.0` and `+nan.0` when the number is inexact by a `#` of the denominator, an exponent or `#i`; a `#` of the numerator alone is no number: `1#/0` is `#f`); `#e1@1` is `#f`
(an exact polar number of a nonzero angle), also in R6RS's; the exponent of any size (beyond 10^6 an exact one is `#f`, an inexact one 0.0 or infinity), the mantissa of 8,000
digits exact. **(2)** `number->string`'s precision: `(number->string z radix p)` writes `|p` after every finite flonum part (Chez's, with `p` at least 53, bignum `p`
too; precision 0 or a non-integer is an error; an exact number with a precision is an error; Chez's accepts every radix with a precision, `(rnrs)`'s only 10); radix 2 to 36
for Chez's (digits to `Z`; a flonum is laid out positionally from radix 16, as Chez does). **(3) Flonum corners**: every NaN is `eqv?` and `equal?` to every NaN, and one key
of a hashtable (Chez's; si compared the bits: `(eqv? (/ 0.0 0.0) +nan.0)` was `#f`, which alone failed about 40 clauses of `fl.ms`, `ieee.ms`, `5_3.ms`: `flmin`, `flmax`,
`div-and-mod` of zeros and infinities, `-`, `sin`, `cos`, `tan`, `magnitude` of NaNs); `fl+` starts at its first argument (`(fl+ -0.0 -0.0)` is -0.0); `remainder` and `modulo` of
flonums return +0.0 for a zero result and the exact 0 for an exact 0 dividend; `expt` of the exact 1 is 1 whatever the power, of a signed zero or infinity to a bignum power keeps its
sign for an odd one, of -inf.0 to an infinite power is NaN+NaNi; `bitwise-rotate-bit-field` takes a bignum count (modulo the width); `sqrt` of a complex with a NaN part and no
infinite one is NaN+NaNi; `exp` of a complex with a large real part scales (`exp 800.0+1e-200i` has the finite imaginary part 2.7263745721125063e147); a flonum complex over an
exact complex divisor divides by its flonum parts when they are finite and nonzero (the exact products overflowed to NaN).

**SMR5, denormals in R7RS mode.** The R7RS mode is the reader's global (`R7RS` in `read.ae`, set from the program's first datum, as the `write` of `(scheme write)` uses it): in it
`number->string` and `write` print a denormal without Chez's `|N` suffix and with `d.0e-N` for a one-digit mantissa (`5.0e-324`, the digits are Chez's shortest, the suffix and the `.0`
are all that changes); R6RS programs keep `5e-324|1`. A `(chezscheme)` program is R7RS-mode too (the reader mode follows the imports), so it prints `5.0e-324` where Chez prints
`5e-324|1`, the one divergence of the rule. The exponent keeps Chez's spelling (`e308`, no `+`): writing `e+308` in R7RS mode would pass the last two tests of the suite but make
every large flonum of every `(chezscheme)` program differ from Chez's output (tried, the whole `bin/si check` run showed it, undone; the two tests stay in the class
`r7rs-float-printing`).

**What moved**, on the mats run (`bin/si mats --record 5_3.ms fl.ms ieee.ms`, 207 mats, light, main's stage 0 as host; before and after):

| | before | after |
|---|---|---|
| `mats-numeric` items | 186 | 2 (`5_3.ms` `expt` 96: a negative flonum to a ratnum power with a huge numerator, Chez gives an infinite complex; `ieee.ms` `expt` 26: `(expt +inf.0+2.0i 2)` is `+inf.0+0.0i` on Chez) |
| `mats-chez-checks` items | 3 | 0 |
| all non-pass items of the three files | 263 | 76 (the rest: `mats-chez-identifier` 65, `mats-arity-one` 5, `mats-chez-reader` 3, `mats-chez-syntax` 1) |
| mats passing / failing / not run | 116 / 26 / 65 | 136 / 6 / 65 |
| failing clauses (`CLAUSEFAIL`) | 139 + 16 + 39 | 6 + 0 + 1 (5 of them `=`, `<` ... `22`, `30`: `mats-arity-one`) |

`bin/si r7rs`: 1,216 of 1,225 before SMR5, now 1,230 of 1,232 (the 9 float-printing tests: 7 pass, 2 are the `e+308` ones; 7 more tests run because the numeric syntax tests now
read); `bin/si srfi` 11 programs, 1,231 checks pass, no regression; `bin/si test --si` 61 of 64 (3 known, 0 failing); `bin/si unit` ok (new: `numbers_ext_test.ae`, 174 of Chez's
answers to `string->number` recorded in `numbers_ext_data_test.ae`, and a test that the strict one keeps R6RS's); `bin/si check` (all 50 programs) 49 of 50, the one known
(`chez-conditions`), no regression; `bin/si check --only test/scheme/essential-check.txt` 48 of 49 (the same one); the new program `si/numeric-corners` (`test/scheme/si/programs/numeric-corners.sps`, 278
expressions of the probes, its Chez run recorded, in the essential subset, in the record by hand as `si/list-checks` was) passes interpreted and compiled (`bin/si check --aot --only si/numeric-corners`,
output equal to Chez's). Not measured: the speed (`eqv?` of flonums has one more test; the NaN keys of hashtables, `canon`), `bin/si ecraven`: the programs that print flonums in R7RS
mode (`(scheme base)`), because of SMR5's `d.0e-N` only on denormals, none expected.

Left, by the probes: `(expt -2.0 100000000000000000001/3)` (Chez `-inf.0+inf.0i`, si `+inf.0`), `(expt +inf.0+2.0i 2)`, `(sqrt -inf.0+nan.0i)`'s sign of the infinite part, `/` of a flonum complex by an exact complex
beyond the flonum range (`(/ 1.0+1.0i (make-rectangular (* 4 (expt 10 400)) (* 4 (expt 10 400))))`: Chez `0.0+0.0i`, si NaN+NaNi), `fx` and `fl` names of the identifier class (SMR6).

**The night's command** (as SMR3's, the records of every file): `bin/heavy-lock bin/si mats --record` (the three files here are recorded; the unrun files' lines are older), `bin/si r7rs --record`,
`bin/si check --record` and `bin/si check --aot --record` for the one new program (written by hand in both records here), then `bin/si ecraven --only` on a few float printers as the measure of the speed.

## 8. As built: SMR6, the Chez identifier tail (2026-11-05, as decided: the stub experiment, then the simple identifiers)

**The experiment.** `SI_MAT_STUB=1` (the mats harness, `test/scheme/mats/run.sps`; off, nothing changes): a clause, a definition or a mat's prelude form that fails with
"unbound identifier X" binds X in the mat's environment to a stub that raises `STUBHIT X` and is evaluated again (up to 40 times each), so a mat runs to its end and says
which identifiers it needs (`STUB MAT X` lines) and which its clauses really call (`STUBHIT X` in a failing clause). All 37 files, 1,075 mats (355 pass, 608 fail, 106 not run
with every identifier stubbed; the 106 are syntax: `fluid-let-syntax`, `define-ftype`, a reader's `#!bwp` as an expression): 1,344 distinct identifiers were stubbed, 588 of them bound in
Chez 10.3.0 (`top-level-bound?` in `chez --script`), the rest names the mats define themselves (a `module` or a macro of the file, pattern variables). Ranking (mats that need the
identifier / mats whose clauses call it / mats where it is the only stub they hit):

| identifier | needed | called | sole | |
|---|---|---|---|---|
| `test-cp0-expansion` | 83 | 83 | 58 | cp0, out of scope |
| `enable-cp0` | 64 | 39 | 18 | cp0, out of scope |
| `optimize-level` | 50 | 36 | 11 | a parameter, now there |
| `separate-eval`, `separate-compile` | 27, 23 | 26, 22 | 2, 0 | compiler, out of scope |
| `collect-maximum-generation`, `collect`, `collect-request-handler` | 26, 29, 14 | 23, 14, 13 | 0, 0, 2 | parameters, now there |
| `load` | 22 | 18 | 0 | source only, now there |
| `cfl~=` | 18 | 15 | 7 | cfl.ms's helper, out of scope |
| `hashtable-values`, `hashtable-cells` | 15 | 14 | 0 | now there |
| `native->signed`, `big-endian->signed` ... (eight names) | 11 to 13 | 11 to 13 | 2 to 4 | a mat-local `module` of bytevector.ms: the harness did not take `module` as a definition |
| `threaded?`, `machine-type` | 11, 8 | 11, 8 | 0, 3 | now there (si has no threads) |

The greedy order by mats completed (a mat is complete when its failing clauses call only the identifiers chosen so far): `test-cp0-expansion` +58, `enable-cp0` +18, `optimize-level` +16, `cfl~=` +10, `run-cp0` +6,
`$suppress-primitive-inlining` +6 (the compiler's, 114 mats of `cp0.ms`, `cptypes.ms`, `cfl.ms`, `7.ms`: out of scope as decided), then `make-record-type` +5, `machine-type` +4, `timer-interrupt-handler` +4,
the mats' own `native->signed` family +4 each, `statistics`, `trace-output-port`, `fxvector` +3 each, ... a tail of 60 names that unlock one or two mats each. 442 mats end with failures only at stubs.

**What was built** (no new primitive: psyntax's boot list and `psyntax.pp.ae` are untouched; everything is Scheme in `scheme/lib/chezscheme.sls`, 146 exports, each probed against Chez 10.3.0 and all bound in it):
`machine-type` (`a6le`) and `threaded?` (`#f`: si has no threads; `#t` made `exceptions.ms` call `fork-thread` and exit the harness, tried, undone), `-1+`, `logior`, `logtest`, `logbit?`, `logbit0`,
`logbit1`, `isqrt` (flonum integers too), `flonum->fixnum`, `fx+/wraparound`, `fx-/wraparound`, `fx*/wraparound`, `fxsll/wraparound`, `fxlogior` and relatives, `fxpopcount*`, `flnonpositive?`, `flnonnegative?`,
`conjugate`, `magnitude-squared`; `sinh cosh tanh asinh acosh atanh` (flonum answers for exact arguments, signed zeros, infinities, the complex answers outside the real range as Chez's; last digit
may differ from libm's); the hashtable names (`hashtable-values`, `hashtable-cells` and the size argument of `hashtable-keys`, `-entries`, `-values`, `-cells`, `$hashtable-cells`, the weak and
ephemeron constructors, `hashtable-weak?`, `hashtable-ephemeron?`, `eq-hashtable-ref` and relatives); `weak-cons`, `ephemeron-cons` and their predicates; `putprop`, `getprop`, `remprop`; `merge!`;
the immutable family (`string->immutable-string`, `vector->immutable-vector`, `bytevector->immutable-bytevector`, `immutable-vector`, `-copy`, `-append`, `-set/copy`, the predicates, `string-append-immutable`);
`vector-copy` (Chez 10.3's start and count), `vector-append`, `vector-set/copy`, `vector-set-fixnum!`, `string-copy!` (Chez's five arguments), `substring-fill!`; `bytevector->s8-list`, `s8-list->bytevector`, the 24, 40, 48
and 56 bit `bytevector-s/u...-ref/set!`; `fxvector` and `flvector` with their operations; `top-level-bound?`, `top-level-value`, `set-top-level-value!`, `define-top-level-value`, `scheme-environment`,
`load`, `record-constructor` of a record-type descriptor and `make-record-type`; `with-input-from-string`, `console-input-port`, `console-output-port`, `console-error-port`, `trace-output-port`, `char-ready?`, `directory-separator`;
and the runtime's parameters with Chez's defaults and no effect: `collect`, `collect-request-handler`, `collect-maximum-generation`, `collect-trip-bytes`, `collect-generation-radix`, `bytes-allocated`,
`timer-interrupt-handler`, `keyboard-interrupt-handler`, `enable-interrupts`, `disable-interrupts`, `optimize-level` (0 to 3), `debug-level`, `enable-unsafe-application`, `print-radix`, `print-gensym`, `print-length`,
`print-level`, `print-brackets`, `print-vector-length`. The harness takes `module` as a definition (bytevector.ms's `native->signed` family and its eight names).

**Where the model differs from Chez** (the classes below): fxvectors and flvectors are vectors (si's reader reads `#vfx(` as a vector since SMR3), so `fxvector?` is true of any vector of fixnums; an immutable
string, vector or bytevector is a marked copy and a mutation of it does not raise (a literal is not immutable; `symbol->string` is not); empty vectors are not one object; the weak tables are strong and marked; the
cells of `hashtable-cells` are fresh pairs; the collector, the interrupts and the printer limits are parameters with no effect. `print-graph` is not defined: with it the mats' `read-graph` writes a cyclic datum through `format "~s"`, which
si writes without labels, and the Go stack overflows (it takes the rest of `6.ms` down, 30 mats). Not built: the file system (`mkdir`, `rename-file`, `directory-list`, `file-directory?`: Go primitives, a psyntax re-boot), the compiler and loader
(`compile-file`, `compile-library`, `load-program`, `expand`, `interpret`), engines, guardians, `oblist`, time and date objects, the foreign interface and the inspector.

**What moved**, the whole mats run (`bin/si mats --record`, 37 files, light, main's stage 0 as host), at the branch before this work (SMR5 built) and after:

| | before | after |
|---|---|---|
| mats | 1,077 | 1,077 |
| pass / fail / not run / skipped | 340 / 62 / 669 / 6 | 436 / 99 / 536 / 6 |
| `mats-chez-identifier` items | 665 | 524 |
| failing clauses of mats not run, run alone | 511 | 729 (more mats stop later) |

Per file (pass / fail / not run, before then after, the files that moved): `bytevector.ms` 11/0/58 to 55/1/13, `5_6.ms` 4/3/34 to 14/14/13, `ieee.ms` 26/1/9 to 35/1/0, `5_3.ms` 71/5/44 to 79/5/36, `fx.ms` 1/0/72 to 6/0/67,
`5_5.ms` 14/7/6 to 18/8/1, `primvars.ms` 1/3/25 to 4/12/13, `6.ms` 5/1/28 to 7/3/24, `fl.ms` 39/0/12 to 41/0/10, `7.ms` 3/2/43 to 5/3/40, `cfl.ms` 0/0/13 to 2/0/11, `8.ms` 7/3/61 to 8/3/60, `5_1.ms` 25/0/3 to 26/2/0,
`misc.ms` 5/1/47 to 6/3/44, `3.ms` 9/0/8 to 10/0/7, `hash.ms` 0/0/34 to 0/2/32; the others by one. 96 mats start and pass; the 37 that start and fail are the new classes: `mats-chez-runtime-params` (187 items: the collector, weak tables,
interrupts and printer limits that si accepts and does not act on), `mats-chez-compiler-loader` (178: `7.ms`'s compile and load interfaces, the profiler, the compiler's output), `mats-chez-immutable-typed` (76: the model's differences above), and the deferred
`mats-after-smr6` (110: mats of `4.ms`, `6.ms`, `8.ms`, `io.ms`, `record.ms` that now run to a clause that fails, by mat). The whole record is rewritten (`test/scheme/check-mats-linux-amd64.edn`, 0 items in no class), so the files this work did not touch carry SMR2 to SMR5's moves as well (the record of those files was older than the code).
After the build, the stub experiment run again: 579 mats still need a stub (668 before), the remainder is the compiler's names and one-mat names (`oblist` 5, `statistics`, `make-engine`, `make-time`, `copy-environment`, `compile-file`, `rename-file`, `mkdir`).

Checks run (light, `ARBACE_HOST_EXE` main's stage 0, no gate; once after the main work and again at the end): `bin/si r7rs` 1,230 of 1,232 (the 2 `e+308` tests of SMR5), no regression; `bin/si srfi` 11 programs, 1,231 checks pass, no regression; `bin/si test --si` 61 of 64, 3 known, 0 failing;
`bin/si unit` ok (no Go changed); `bin/si check` (all 50 programs) 49 of 50, the one known (`chez-conditions`), no regression; `bin/si check --aot --only test/scheme/essential-aot.txt` 10 of 11 the compiled run the same as the interpreted
one, 1 refused (`racket/syntax-case`, a constant that does not read back: as before). Not measured: the speed of `(chezscheme)` programs' load (the library has 146 more definitions: its expansion is a little longer; `bin/si bench` at the night).

**The night's command** (as SMR3's and SMR4's): `bin/heavy-lock bin/si mats --record` (the whole record again; this branch's is from a light run on the day), `bin/si r7rs --record`, `bin/si check --record` and `bin/si check --aot --record`
(unchanged programs), and the stub experiment again for what is left: `SI_MAT_STUB=1 bin/si mats FILE.ms...` (a partial run; its `.tmp/si/mats/FILE.out` then holds the `STUB MAT X` and `STUBHIT X` lines; judge nothing from it and do not `--record` it), read by counting those lines per mat.

## 9. As built: SMR7 (2026-11-06; completeness of the mats, psyntax booted on si, Chez's format floats)

**The 536 not run, by first obstacle** (the SMR6 record, then the stub experiment run again on this branch, `.tmp/a/stub-rank-base.txt` of the work):
the cp0 compiler mats (`test-cp0-expansion` 71 mats, `expand/optimize` 26, `enable-cp0`, `equivalent-expansion?` of expansions; a design class as decided,
SMR7a); Chez's own record system (`define-record`, typed fields, `csv7:`: record.ms entirely, and uses in 6.ms, 8.ms, misc.ms); mat.ss's helpers the
harness did not give (`cfl~=` and `*fuzz*` alone held 21 mats of 5_3.ms); Chez's lexical syntax still unread (`#{name uid}` gensyms, `#\rubout`, `#\377`,
`#5(`, `#36r`, Chez's number syntax after `#`); the files' structure (foreign.ms's mats inside a `meta-cond`; 8.ms's libraries defined in clauses); and the
one-mat tail of Chez identifiers.

**What was built.**
- `scheme/lib/si/chez-records.sls` (new, re-exported by `(chezscheme)`): `define-record` (parent, default fields seeing the fields before them, the options
  `prefix`, `constructor`, `predicate`, `nongenerative`, `sealed`, `opaque`; duplicate field names refused), typed fields (integer-N and unsigned-N take
  -2^(N-1) to 2^N-1 and keep the low N bits, double-float and single-float a flonum, single-float rounded, fixnum, boolean, char, wchar),
  `make-record-type` with Chez's field specifications (a gensym name is the uid), `record-constructor` converting typed fields, the `csv7:` accessors,
  mutators, field lists and declarations, `type-descriptor`, `record?` with an rtd, `make-record-type-descriptor` with Chez 10's `(N . mask)` fields.
  The model: an R6RS record type with the fields' mutability, the types and the constructor's arity in a table beside it.
- `(chezscheme)`: the cflonum operations; the compiler's parameters with no effect (`enable-cp0`, `run-cp0`, `$suppress-primitive-inlining`, ...); the
  comparisons of one argument (`=` `<` `char=?` `string=?` and the rest: `mats-arity-one`); destructive `reverse!`, `subst!`, `remq!`, `remv!`,
  `remove!` and a sharing `subst` (`mats-chezscheme-subset`); `random-seed` (the generator's state is now Chez's 32-bit seed), the cas operations, Chez's
  five-argument `vector-copy!`, `expt-mod`, `flsingle`, `flbit-field`, `datum`, `syntax->list`, `syntax->vector`, the index checks, and parameters.
- `(si mats)`: mat.ss's helpers (`*fuzz*` settable as mat.ss's variable, `cfl~=`, `equivalent-expansion?`, `mkfile`, `$record->vector`, `find-source`,
  `retry-for-spurious`, the machine predicates); `(scheme)`, Chez's name of its system library (`scheme/lib/scheme.sls`, generated from
  `(chezscheme)`'s export list).
- The harness (`test/scheme/mats/run.sps`): a `meta-cond` at the top level takes its chosen clause's forms as top-level data; a `library` form in a clause
  (or in a top-level `begin`) is expanded and installed, as Chez's REPL does.
- The reader with ChezLex (`go/arbace/scm/read.ae`, `aot.ae`): `#{name uid}` and `#{name}`, the character names `rubout` `bel` `nel` `ls` and `#\NNN`
  octal, `#N(`, `#Nvu8(`, `#Nvfx(`, `#Nvfl(`, `#Nr`, and Chez's number syntax after `#` (`#x10000000000000.0`, `#b1.1e1001100`).
- `expt` of a flonum to an integer beyond 64 in magnitude is the correctly rounded power (`pnPow`), not repeated squaring (thousands of ulps away at 2^56).

**SMR7b, psyntax booted on si itself** (as decided). `tools/psyntax/boot-si.sps` is boot.ss's program run by si's own psyntax (the committed
`psyntax.pp` running on the light driver) instead of the vendored psyntax loaded into Chez: the libraries' export lists are the running system's
(`environment-symbols`: Chez's `library-exports` at the first boot), the bootstrap libraries reuse the running system's labels (psyntax's bless'd
identifiers are cached with them: fresh labels made `...` an ordinary identifier, "with-syntax: duplicate binding"), and every gensym is renamed
`NAME$N` in the order met. `bin/si boot` runs it (16 s, no Chez); its first output equals the Chez-booted `psyntax.pp.ae` up to the gensyms' names
(5,056 gensyms, the forms otherwise identical), and it is committed: booted again from itself it gives the same file byte for byte (`bin/si boot
--check`: "the bootstrap gives the committed file"). `bin/si fixpoint` (Arbace expanding the sources) still gives the same six libraries. The gate's
`si-check` no longer needs the host `:chez`. New primitives are possible now: add the name to boot-si.sps's `si-primitives` and boot.

**SMR7c, Chez's format floats** (as decided; `scheme/lib/si/format.sls`, written for Arbace from CLHS 22.3.3 and the format.ms mats): `~w,d,k,ovf,padF`,
`~w,d,e,k,ovf,pad,expE`, `~w,d,e,k,ovf,pad,expG` over a number as its digits and a significance threshold: ~F takes the shortest digits strictly inside
the flonum's rounding interval (so 1e23 is 9999999999999999 and seven #), a position whose place is at most a tenth of the ulp prints as `#`, the
leading and trailing zeros of the field are optional as room allows (SBCL's lpoint and tpoint), a width without d rounds to the digits that fit; ~E
takes the printer's shortest digits, k digits before the point (or -k zeros after it), the exponent padded to e digits, the field overflowing on an
exponent too long or a d too small for k, a width rounding once (a carry is not refitted); ~G chooses ~F with ww = w - ee when 0 <= d - n <= d (n from
the value rounded to d digits) and pads ee spaces only with a width. All 540 float clauses of format.ms pass (format-fixed1..8, exp1..4, general, dollar,
slib24..27, errors), and with them `~r` in every radix (and of a list: "(A B C)"), the radix ten when only other parameters are given, and `~<` as
CLHS has it (the `~n,m:;` prefix written only when the field passes the line, the segments a `~^` ends left out).

**Moved** (a light run on the day, the 37 files five at a time on one key, the record written from its outputs, `bin/si corpora mats .tmp/si/mats --key K --record`; 0 items in no class, the new ones in `mats-after-smr7`, `mats-format` now has none):

| | SMR6 record | this branch |
|---|---|---|
| mats | 1,077 | 1,125 (more mats read: the reader, the meta-cond) |
| pass / fail / not run / skipped | 436 / 99 / 536 / 6 | 560 / 89 / 470 / 6 |

Per file (pass / fail / not run, record then now): `3.ms` 10/0/7 to 11/0/8, `5_2.ms` 31/15/10 to 45/5/6, `5_3.ms` 79/5/36 to 99/5/16, `5_4.ms` 21/6/5 to 28/2/4, `5_5.ms` 18/8/1 to 24/3/0, `5_6.ms` 14/14/13 to 19/26/5, `5_7.ms` 1/2/4 to 2/3/3, `6.ms` 7/3/24 to 10/4/22, `7.ms` 5/3/40 to 6/3/39, `8.ms` 8/3/60 to 10/4/60, `bytevector.ms` 55/1/13 to 65/3/12, `cfl.ms` 2/0/11 to 9/0/4, `cp0.ms` 0/0/14 to 1/0/13, `cptypes.ms` 0/0/24 to 1/0/23, `fl.ms` 41/0/10 to 43/0/8, `foreign.ms` 0/0/20 to 0/0/21, `format.ms` 43/14/0 to 61/1/0, `ieee.ms` 35/1/0 to 35/1/2, `misc.ms` 6/3/44 to 10/3/42, `oop.ms` 0/0/0 to 0/0/1, `primvars.ms` 4/12/13 to 10/16/3, `record.ms` 0/0/30 to 15/1/21.

Checks run (light, `ARBACE_HOST_EXE` main's stage 0): `bin/si unit` ok; `bin/si r7rs` 1,230 of 1,232 (the 2 `e+308`), no regression; `bin/si srfi` 11
programs, 1,231 checks, no regression; `bin/si test --si` 61 of 64, 3 known; `bin/si test` (the bootstrap host) ok; `bin/si check` 49 of 50 (the one
known), no regression; `bin/si check --aot --only test/scheme/essential-aot.txt` as before (1 refused, the known); `bin/si fixpoint` 6 of 6; `bin/si boot
--check` the committed file.

**Left.** The cp0 mats (design); `record-reader` and `#[...]` with Chez's record printing, real fxvector, flvector and box types, the file system
(`mkdir`, `directory-list`), engines, guardians, `oblist`, time objects: Go primitives, now possible without Chez (SMR7b); Chez's `module` export forms
(`(($a $b) ...)`, implicit exports) in psyntax; `$hashtable-veclen` (Chez's resizing policy); the compile and load interfaces (7.ms). The night's
command, to confirm the day's record on one invocation: `bin/heavy-lock bin/si mats --record` (expect 560 / 89 / 470 / 6), then `bin/si r7rs`, `check`, `check --aot`.

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

## Decided (2026-11-05, the user)

SMR1-SMR8 as recommended: include in the Chez variant with a program and its Chez run at the next night; the list-argument checks; the mats' reader resynchronised, then Chez's four syntaxes; string->number's extended syntax and the precision argument first; denormals printed without Chez's |N suffix in R7RS mode, Chez's digits kept; the identifier stub experiment, then the simple identifiers; si's speed after the tag; the mutable interaction environment left until a corpus needs it.

## Proposed (SMR7)

- **SMR7a the cp0 compiler mats** (`test-cp0-expansion` 71 mats, `expand/optimize` 26, `equivalent-expansion?` of expansions): (a) a design class, si has
  no cp0; (b) a port of Chez's cp0 and of its expander's output format. *Recommended: (a)*.
- **SMR7b psyntax booted on si**: new primitives (`record-reader` and `#[...]`, real fxvector, flvector and box types) need psyntax's boot list
  changed, and `bin/si boot` runs psyntax on Chez. (a) boot psyntax on si itself, `bin/si fixpoint`'s expansion as the boot, Chez out of the
  build; (b) keep Chez's boot. *Recommended: (a)*.
- **SMR7c Chez's format float directives** (`mats-format`, 14 failing mats): (a) port Chez's algorithm for `~f ~e ~g` (digits past the flonum's
  precision as `#`, the width rules), 2 to 3 days; (b) leave the class. *Recommended: (a)*.

## Decided (2026-11-06, the user: SMR7a (a), SMR7b (a), SMR7c (a))

The cp0 compiler mats stay a design class; psyntax is booted on si itself (`bin/si fixpoint` as the boot), removing Chez from the build; Chez's
format float directives `~f ~e ~g` are ported, Chez's algorithm.
