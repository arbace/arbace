# OCaml as a fifth crop: survey and plan

Status: survey and plan, 2026-10-30, at the user's request. Nothing here is adopted: no code under
`arbace/`, `go/`, `bin/` or `test/` changed, and only this file was written. The decisions are the
user's, as proposals OC1-OC16 (§12); the milestones are O1-O9 (§11), named by OC1. It follows
doc/C23-SURVEY.md's shape (the latest crop survey) and doc/FULL-SUPPORT-SURVEY.md's matrix.

Base: branch `ocaml-survey` from main `68b2bba6`. GT7's clean run was going, so the work is reading
and docs plus probes of a few seconds each (`ocamlc -version`, `-dparsetree`, `-dsource`, `-dlambda`,
`-dtypedtree` on files of a few lines; scratch in the worktree's `.tmp/ocaml/`). No Arbace executable
was built or run, no lock taken, no package installed, nothing downloaded.

Words used throughout:

- **OCaml forms**: OCaml modules written as Arbace forms (§4), the counterpart of Go forms, Java forms,
  C forms.
- **repr/ocaml**: the bridge between OCaml text and OCaml forms (`arbace.repr.ocaml`,
  `bin/repr-ocaml read`, `print`), as repr/go and the parked repr/haskell are.
- **oi**: the interpreter of OCaml forms at the REPL (name proposed by OC2), as goi is Go's.
- **The oracle**: the host's OCaml 5.5.0 compiler (`ocamlc`, and its library `compiler-libs`).

Contents: Summary · 1 What parity means · 2 The host and prior art · 3 Measurements · 4 OCaml forms ·
5 repr/ocaml: read and print · 6 eval: oi · 7 AOT · 8 The oracle and the corpora · 9 Licenses ·
10 The fifth row · 11 Plan (O1-O9) · 12 Proposals (OC1-OC16) · 13 Order and what to decide first ·
14 Risks · 15 Sources.

## Summary

- **The host has OCaml 5.5.0** (`/usr/bin/ocaml`, `ocamlc`, `ocamlopt`; Alpine edge package `ocaml5-5.5.0-r0`,
  amd64, musl, **no flambda**), the compiler the bytes of go-whim's whiml were built with. Not installed:
  `ocaml5-compiler-libs` (70 MiB installed), without which no helper can link the compiler's own parser;
  dune, opam, ocamlfind are absent. The compiler's printers (`-dparsetree`, `-dsource`, `-dtypedtree`,
  `-dlambda`, `-dcmm`) work from the CLI today. Installing one apk is the first decision (OC3).
- **OCaml is a better-shaped crop than C and a harder one than Go.** Syntax is regular and the compiler's
  Parsetree is a stable, small, documented data type (about 100 constructors, the same family as Go's
  `go/ast`); the semantics need Hindley-Milner inference with modules, functors, objects, GADTs and
  type-directed disambiguation, and the runtime is OCaml 5's multicore one with effect handlers. The
  Parsetree is **lossy** (probe E1: comments and parenthesization are gone; `-dsource` re-prints it in the
  compiler's own style), so read/print `:full` needs Arbace's own lexer, as repr/go's mode `:full` and
  repr/haskell's did (§5.3).
- **Prior art in go-whim is a consumer, not an implementation.** whiml (`whiml/`, 58,346 lines of OCaml,
  57,093 of them generated `editor.ml`) is vim's editor core emitted as OCaml by `crefactor/togo`'s ML
  backend (about 4,100 lines of Go), built by `ocamlopt` module by module; it shows what the host compiler
  accepts and uses `Domain`, not effects. It has no OCaml parser. So the oracle pattern is repr/haskell's
  `tools/hsdump` and repr/rust's `tools/rsdump`, a helper in the language on the compiler's library.
- **Recommended shape (§4-§7).** OCaml forms in the Go forms' vocabulary; repr/ocaml with a helper
  `tools/ocdump` (OCaml, on compiler-libs, dumping Parsetree and, later, Typedtree) as the oracle for
  read, a native lexer/parser later as repr/java's JN8 and repr/go's GP5 did; **types taken from the
  oracle** for eval and AOT in v1 (a Typedtree dump), with an Arbace type checker a later, separate crop
  of effort (OC4); eval as an interpreter of **Lambda** (the compiler's own pre-optimization IR, `-dlambda`),
  not of the Typedtree (OC5); effects by delimited continuations on the runtime's coroutine machinery (OC6);
  AOT as Lambda → Java forms → j2g.
- **Plan (§11): O1-O9, about 37 to 63 working days** with types from the oracle (O4 is the oracle's
  Typedtree and Lambda dumps, not a checker); an Arbace checker would add about 15 to 30 (OC4 (b)).
- **To decide first (§13):** the version pin (OC1), the apk (OC3), types (OC4), eval's representation
  (OC5), the order against C23, net/http and Scheme (OC15).

## 1. What parity means

Under the three crops' rule (doc/FULL-SUPPORT-SURVEY.md, "The binding decisions" and "REAP v1's
divergences"): on the host (linux/amd64), forms only, each level's results recorded as data,
`bin/reap` judging from the records with no runs, none failing, every non-pass in a recorded class.

| cell | in Arbace (proposed) | corpus | reference | judge |
|---|---|---|---|---|
| **OCaml read** | repr/ocaml `read` | stdlib, testsuite, opam set (§8.2) | `ocamlc`'s acceptance, error text and Parsetree (`-dparsetree` / `compiler-libs`) | trees equal to the helper's as data; the forms printed (`print`) and compiled by `ocamlc` give the original's `.cmi`/`.cmo` bytes (the AST-level and object-level judges, as repr/haskell's interface level) |
| **OCaml eval** | oi (Lambda interpreter) | the run programs (testsuite's `.ml` with `.reference`) | `ocamlopt` build and run of the originals (status, stdout, stderr) | byte for byte, as goi's |
| **OCaml AOT** | Lambda → Java forms → Go (OC7) | as eval | as eval | as eval |
| **OCaml print** | repr/ocaml `print` (`:lines`, `:full`) | every corpus | the original text; the forms read back; the object | `:full` byte for byte; `:lines` forms → OCaml → forms equal, its run equal |

What differs from the crops:

- **There is a reference compiler and runtime on the host, as Go's and Java's were**, so recorded runs
  need not be kept as data (C's problem, C23-SURVEY §1); but the standalone direction wants the check
  free of Alpine's packages, so runs are recorded as data anyway (`--record-ref`, as Java-check does).
- **The meaning is not in the syntax.** `a.(i)`, `x.f` (record-field disambiguation), `M.(e)`, `let open`,
  overloading by the expected type (`~f:(fun ...)`, polymorphic comparison, `format` strings typed
  as `('a,'b,'c) format`) need types. The read level can keep the syntax; eval cannot (§6).
- **OCaml's standard library is OCaml** (`stdlib/*.ml`, about 70 modules), plus a C runtime
  (`runtime/*.c`, bytecode and native). Eval needs primitives for the C part (`caml_*` externals), a
  list of a few hundred (unmeasured: the sources are not on the host; §3 gives what
  could be measured).

## 2. The host and prior art

### 2.1 The host

- `ocaml5-5.5.0-r0` (Alpine edge main), `LGPL-2.1-or-later WITH OCaml-LGPL-linking-exception`;
  `ocaml5-runtime-5.5.0-r0` likewise; `ocaml5-compiler-libs-5.5.0-r0` available, not installed (70 MiB
  installed); `ocaml5-doc`, `ocaml5-ocamldoc`, `dune-3.22.2` (MIT), ctypes, integers also in the repo.
- `/usr/lib/ocaml` has 183 entries: `stdlib.cmxa`, `.cmi`, `.cmx`, `caml/*.h` (runtime headers), **no `.ml` or
  `.mli` sources** (0 `.mli` found). The stdlib as a corpus therefore needs the source tarball
  (upstream ocaml/ocaml at tag `5.5.0`, or Alpine's aports `main/ocaml5` APKBUILD, which names the
  tarball and its sha512); the sizes of both are not measured here (no download), the upstream tarball
  is of the order of 5 MB xz, the git tree of the order of 100 MB, the testsuite a few thousand files
  (all to be measured at O1 and pinned, OC1).
- `ocamlopt -config`: version 5.5.0, architecture amd64, **flambda false**, so `-dlambda` is the last
  IR before Cmm (`-dcmm`); `-dclambda` (flambda off) exists too. With flambda there would also be
  `-dflambda`; Alpine's build does not have it (OC7).
- `ocaml5` is the package name since 5.x coexisted with `ocaml4` in Alpine (an `ocaml-4.14.4-r0` is
  the other installable); the pin must name the package (OC1).

### 2.2 go-whim (`/root/go-whim`)

- `whiml/` (`editor.ml` 57,093 lines generated, `rt.ml` 317, `host.ml` 127, `term.ml` 162,
  `snprintf.ml` 641, `main.ml` 6): `doc/OCAML.md` (321 lines) built 2026-10-04: "OCaml 5.5.0, native
  (`ocamlopt`, amd64, musl), without flambda; `Domain`, `Atomic`, `Mutex`, `Condition` for the parallel
  `:%s`, and `unix`"; "no dune, no ocamlfind, no opam: nothing was installed", `ocamlopt` called module
  by module from Go. 80 of 80 and 240 of 240 cases as the C does, 0.5-0.6 of the C's time on the heavy
  case. It is the proof the host builds a 60k-line OCaml program, and a large generated **corpus** (but
  C-in-OCaml's style: one `Bytes`, pointers as ints, no variants or modules).
- `doc/OCAML-IDIOMS.md` (351 lines): the same backend made idiomatic (variants, records, OCaml strings
  where types prove them sound): a second corpus style.
- `crefactor/togo/ml.go`, `ml_expr.go`, `ml_doc.go` (Wadler-style printer), `ml_enum.go`, `ml_record.go`,
  `ml_test.go`: the OCaml **printer** from Scheme forms, 4,090 lines of Go. Reusable idea: a layout
  engine for OCaml (precedence-aware, `begin`/`end`, parenthesization). Not reusable as a reader.
- Not in go-whim: any OCaml parser, type checker or interpreter; `doc/GHC-LISP.md` is the closest
  analogue (Haskell as s-expressions), and `doc/LISP-SANDBOX.md`.

### 2.3 How Arbace did the parked bridges

repr/haskell: `tools/hsdump` on the host's GHC (`ghc` library, pinned by `tools/hsdump/GHC_VERSION`), a
converter `arbace.repr.haskell.convert` and printer `.print` in Arbace, the round trip at levels
(forms, text `:full`, interface via `--iface`), corpus GHC's boot libraries pinned
(`test/haskell/corpus-ghc-9.14.1.edn`). repr/rust: `tools/rsdump` (a `rustc_private` driver, needing
`rustc-dev` as an apk fetched into `.tmp/` and pinned by `tools/rsdump/apks.sha256`). Both parked
2026-10-26. The same pattern fits OCaml with one difference: compiler-libs is an ordinary library
(no private driver), `ocamlfind` is absent but `ocamlc -I +compiler-libs ocamlcommon.cma` links it.

### 2.4 Compiler-libs

The compiler's own library `compiler-libs` (`ocamlcommon`): `Parse`, `Parsetree`, `Ast_helper`,
`Pprintast`, `Typedtree`, `Typemod`, `Lambda`, `Translcore`, `Printlambda`, `Location`, `Env`, `Types`,
`Typecore`, `Cmi_format`. The helper links it, parses with `Parse.implementation`/`interface` on a
`Lexing.lexbuf`, and dumps the tree with its own S-expression printer (not `-dparsetree`, which is a
display format with positions and omits `ghost` and docstring details a round trip needs, E1). The
helper must be built by the host's `ocamlopt`: that is "the compiler runs the oracle", as `ghc` runs
hsdump.

## 3. Measurements

Probes (seconds, in `.tmp/ocaml/`):

- **E1, the Parsetree is lossy.** `ocamlc -stop-after parsing -dsource` on `let x = (1 + 2) * 3 (* c *)` prints
  `let x = (1 + 2) * 3`: the comment gone, the parentheses re-derived from precedence (`(a +. b) |> ignore`
  for `a +. b |> ignore`, adding parentheses the source did not have). `-dparsetree` of a 6-line file with
  a PPX attribute, optional argument, polymorphic variant pattern, `let%ext`, functor, class and GADT was 255
  lines; it printed the type declaration with `attribute "deriving"`, `ptype_kind = Ptype_variant`,
  positions as `file[line,byte-of-line-start+col]` pairs; the `let%ext` failed typing ("Uninterpreted
  extension 'ext'"), as expected with no PPX. So: attributes and extensions are in the tree; parentheses
  and comments are not (docstrings are, as attributes).
- **E2, Lambda is readable.** `ocamlc -dlambda -c u.ml` for `let x = (1+2)*3 let f a b = a +. b |> ignore`:
  `(setglobal U! (let (x/274 =[int] (* (+ 1 2) 3) f/275 = (function a/277[float] b/278[float] : int (ignore (+. a/277 b/278)))) (makeblock 0 x/274 f/275)))`.
  Constant folding does not happen at this level; types appear as kinds (`int`, `float`) only; `|>` is already
  inlined (a primitive). `-dtypedtree` prints names with stamps (`"x/274"`) which the Lambda uses too.
- **E3, what a sizing needs and could not be measured.** The stdlib and testsuite sources are not on the host,
  so line counts of `stdlib/*.ml` (about 70 modules) and the testsuite are not measured; they are the first
  task of O1 (pin the tag, count).
- **E4, the toolchain on the host**: `ocamlc`, `ocamlc.opt` 9.7 MB, `ocamlopt`, `ocaml` toplevel 28 MB,
  `ocamllex`, `ocamldep`, `ocamlcmt`, `ocamldebug`, `ocamlcp`.

## 4. OCaml forms

### 4.1 Principles

The same as the other crops' (doc/go/SPEC.md, doc/haskell/SPEC.md): data readable by the Clojure reader,
one form per syntactic category, positions in mode `:full` only, names spelled as the source; and
compatible with Arbace's Clojure-extension aim (a form is `(head args...)`, keywords for flags, vectors
for lists, maps for records). The proposed head vocabulary follows the Parsetree's constructor names
lower-cased with `-` (`Pexp_apply` → `apply`), which makes the oracle's dump and the converter a
near-identity (OC8).

### 4.2 The Parsetree's shape (the facts forms must carry)

- **Structure and signature.** `.ml` is a `structure` of `structure_item`s (`let`/`let rec`/`let ... and`, `type`
  (recursive groups, `type nonrec`), `type t += ...` (extensible), `exception`, `module`, `module rec`, `module
  type`, `open`, `include`, `external`, `class`, `class type`, `Pstr_eval`, `Pstr_extension`, `Pstr_attribute`);
  `.mli` is a `signature` of `signature_item`s (`val`, `type`, `module`, `module type`, `external`, ...). Two
  top forms: `(ocaml/impl ...)` and `(ocaml/intf ...)`, as Go has package and Java compilation unit.
- **Expressions** (about 35 constructors): constants (int/char/string/float with suffix and the quoted
  form `{id|...|id}`, which keeps its delimiter id), identifiers (`M.x`, `M.(+)`), `let`/`let rec`/`and`,
  `fun`/`function`, application with **labelled** (`~f:e`), **optional** (`?f:e`) and punned (`~f`) arguments,
  `match`/`try` with guards and or-patterns, tuples, constructors (`Some x`), polymorphic variants
  (`` `A e ``), records (with `with`), field access and update, arrays (`.( )`, `.[ ]`, `.{ }` indexing
  operators and the user-defined `.%[ ]`), `if`, sequences, `while`/`for`, constraints `(e : t)` and coercions
  `(e :> t)`, `lazy`, `assert`, `new`, `object ... end`, method call `o#m`, instance variable set `x <- e`,
  `let module`, `let open`, `(module M : S)` packing, `letop` (`let*`, `let+`, `and*` with operator spelling),
  and `Pexp_extension`.
- **Patterns:** any, var, alias, constant, interval (`'a'..'z'`), tuple, construct, variant, record (with `_`
  open flag), array, or, constraint, type pattern `#t`, lazy, unpack `(module M)`, exception, effect (OCaml
  5.3, `effect e, k ->`).
- **Types:** variables, arrows with labels, tuples, constructors, objects (`< m : t; .. >`), classes `#c`,
  alias `as 'a`, **polymorphic variants** (`[ `A | `B of t ]`, `[> ...]`, `[< ... > ...]`), `'a. t` poly,
  package `(module S)`, extension. Type declarations: abstract/variant/record/open, **GADTs** (constructors
  with result types `: int -> int g` and existentials `type a. ...`), private, manifest, constraints, variance
  and injectivity (`+'a`, `!'a`), `type a. ` locally abstract types.
- **Modules and functors:** `module F (X : S) : T = struct ... end`, applicative functors `F(X).t`, generative `functor ()`,
  `with type t = ...` / `with module`, destructive substitution `:=`, first-class modules, recursive
  modules, `module type of`, `include (M : S)`, `open!`/`open struct ... end`.
- **Objects and classes:** `object (self) inherit c as super; val mutable x; method private m; initializer ... end`,
  virtual/private/inherit, class types, class applications and class functions, `new`, `#m`, `{< x = 1 >}`.
- **Attributes and extensions:** `[@a]` (on the item or expression or type), `[@@a]` (item), `[@@@a]` (floating), and
  `[%e]` / `[%%e]` / `let%e`, `[%e: t]` and `[%e? p]` payloads (structure, signature, type, pattern). PPX
  rewriters live outside the compiler (driver `ppxlib`), so the forms are the **pre-PPX** syntax (an
  `[@@deriving ...]` stays an attribute) and a PPX's expansion is out of v1's scope (OC9).
- **Docstrings** `(** ... *)` are attributes `ocaml.doc` in the Parsetree, positioned.
- **Operators and spelling:** infix/prefix operator names (`+`, `|>`, `**`, `@@`, `!=`, `:=`, `land`, `lsl`, `mod`,
  `or`, user-defined `>>=`, `let*`, `.%()`), spelled as the source in a symbol form; `( + )` and `(+)` and
  `(  +  )` are the same tree (E1), so spelling beyond the parsed names is `:full`'s.

### 4.3 What forms add or defer

- Locations: the Parsetree's `loc_start`/`loc_end`/`loc_ghost` kept in `:full`, ghost locs flagged (the compiler
  invents ghost nodes for sugar: `fun x y ->` nesting, `[1;2]`'s `::` chain, `(a, b)` for unparenthesised
  tuples).
- The sugar: lists `[1;2]`, `if` without `else`, `a.b.c`, `x.f <- e`, string escapes, `let f x = ...` (a
  `Pexp_fun` inside a binding with the ghost), comment-position attributes. The forms keep the sugared
  shapes (what the parser outputs), not a desugared core (Lambda is the desugared one, §6).
- A fixed list of reserved and operators; a lexer built from `ocamllex` rules (`lexer.mll`) is the source for the native
  lexer (§5.3).

Examples (proposed, to fix at O1):

```
(let [(f [(opt x 1) (label y) (variant Foo z)] (+ (+ x y) z))] ...)      ; let f ?(x=1) ~y (`Foo z) = x + y + z
(type t [a] [(A (a)) (B)] {:attrs [(deriving x)]})                       ; type 'a t = A of 'a | B [@@deriving x]
(module F [(X (sig (type t)))] (struct (type u [] (list X.t))))          ; module F (X : sig type t end) = struct type u = X.t list end
```

## 5. repr/ocaml: read and print

### 5.1 The pipeline

`bin/repr-ocaml read FILE` → helper `tools/ocdump` (OCaml, compiler-libs) → S-expression dump → Arbace's converter
→ OCaml forms; `print` → the printer (a layout engine, borrowing `crefactor/togo/ml_doc.go`'s idea) →
OCaml text. Native reader later (§5.3).

### 5.2 Modes

`:lines` (the forms printed in a canonical layout; forms → text → forms equal) and `:full` (the text
byte for byte). Levels of the round trip, from repr/haskell's: forms (text → forms → text → forms), `:full`
(text), the `.cmi`/`.cmt` level (the printed file compiled by `ocamlc`, the `.cmi` bytes equal, since the
`.cmi` holds the signature digest; `.cmt` for Typedtree), and the behaviour level (programs of §8.2
built and run from the original and the print).

### 5.3 Why `:full` needs Arbace's own lexer

The Parsetree has no comments, no parenthesization, no whitespace, no `begin`/`end` versus parens, no
`0x1F` vs `31` (constants keep their literal text in `Pconst_integer (string, suffix)`, so that part is
kept), no `;;` and no spelling of `fun`/`function`/`->` sugar. The compiler's lexer (`lexer.mll`, ocamllex)
yields tokens with `Lexing` positions and keeps comments in a list (`Lexer.comments ()`), and the
parser's **docstring** machinery keeps `(** *)`. So the helper dumps (a) the tree, (b) the comment list
with locations, (c) the token stream with locations (`Lexer.token`); Arbace's converter rebuilds
layout from tokens and puts comments back by position. A native lexer and parser (a port of
`parser.mly`, Menhir-generated with ~900 productions, to a hand-written recursive descent, or a
Menhir-table interpreter) is the JN8/GP5 step (OC2, milestone O6a, optional).

### 5.4 Errors

Syntax errors judged by `ocamlc`'s message and location (`Syntaxerr`, `Lexer.error`): the helper catches
the exception and dumps `(error kind loc msg)`; a file with an error is a corpus case, judged on the
message as goi's errorcheck tests are. Type errors are the checker's (OC4).

### 5.5 `.mli`, the order of compilation, ocamldep

`.ml` and `.mli` read separately; `ocamldep` (on the host) orders modules; repr/ocaml's `check` runs by
a project's dependency order (`bin/repr-ocaml check DIR`).

## 6. eval: oi

### 6.1 What eval means for OCaml

A REPL (`bin/oi`, the `ocaml` toplevel's behaviour: `# let x = 1;;` → `val x : int = 1`) and program
runs. Reference: `ocaml`/`ocamlopt` on the originals; the judge: status, stdout, stderr's digests,
as goi's. The toplevel's printing of values (`val x : int list = [1; 2]`) uses types and
`Printtyp`/`Toploop` (type-directed value printing via `Genprintval`), a second judge (OC12).

### 6.2 What is hard

1. **Types for semantics.** Type-directed disambiguation (record fields and constructors with the same name across
   modules chosen by the expected type), first-class module packing, GADT pattern refinement, labelled/optional
   argument elision (`f ~x` vs `f x` under partial application: whether `?x` defaults is a function of the *type*),
   polymorphic comparison/hashing specialization (`compare` on ints vs generic compare via the runtime), `Printf`'s
   format strings (`"%d"` typed as a format at the *type level*), the value restriction, and runtime
   type tags for `float` arrays and unboxed floats in records (the representation depends on the type: `float array`
   unboxed, `'a array` not). A syntax-only interpreter would run wrong. Hence Typedtree or Lambda as the
   input (OC5), which has already resolved all of those.
2. **Effects (OCaml 5).** `Effect.perform`, deep and shallow handlers with one-shot continuations
   (`continue`, `discontinue`); the runtime uses separate fiber stacks. In an interpreter on Arbace's runtime (a
   Go-hosted program), an effect handler is a delimited, one-shot continuation: implementable by running the
   handled computation on its own goroutine and passing control by channel (one-shot only, so channels
   suffice; `Obj.clone_continuation` is gone in 5.x), or by CPS in the evaluator (slow, large). OC6.
3. **The GC and runtime model.** Values are tagged words: `int` 63-bit (a wrap at 63 bits is observable), blocks with tags
   (`makeblock 0 x f`), `Obj.magic`, `Obj.repr`, `Weak`, `Ephemeron`, `Gc.finalise`, `Gc.stat`. An interpreter holds
   OCaml values as Go objects: int as a masked 63-bit, blocks as a slice and a tag; `Obj` operations are a
   by-design divergence class (OC11). Finalisers and weak pointers run on the Go GC, so their timing differs
   (class). Multicore: `Domain.spawn` = goroutines, `Atomic` and `Mutex` straightforward; data races
   have defined (not UB) semantics in OCaml 5's memory model, which the interpreter cannot promise to match
   (class).
4. **The C runtime and stdlib.** `external` declarations (`caml_array_make`, `caml_string_blit`, `caml_sys_*`,
   `%`-primitives like `%addint` which the compiler inlines, `caml_ml_*` channels, `unix` and `str` libraries
   with C stubs). The `stdlib/*.ml` is OCaml (interpretable), the externals are a table (a few hundred,
   unmeasured, E3) in Go, like jrt for Java.
5. **Exceptions and tail calls.** Proper tail calls (the interpreter needs a trampoline or a loop in Go),
   `try ... with` and `raise` (cheap in OCaml; Go's panic/recover is slow, so exceptions as a returned status in
   the evaluator, as goi did for Go's panics), `Stack_overflow`, `Out_of_memory`.
6. **Evaluation order.** OCaml's native order is right-to-left for function arguments and tuples (unspecified,
   observable: `f (print_string "a") (print_string "b")` prints `ba`); Lambda has it *already made explicit*
   by `let` sequencing in the translation, which is the case for Lambda (§6.3).

### 6.3 Typedtree versus Lambda

- **Typedtree** (`-dtypedtree`): full types, all names resolved with stamps, modules/functors explicit,
  pattern matches **not compiled**, labelled-argument application not yet reordered (done in `Translcore`/`Typecore`
  with the expected types), classes not translated. Interpreting it means reimplementing `Translcore`,
  `Matching` (match compilation, though an interpreter can do naive matching), `Translmod` (functors),
  `Translclass` (objects); gives the best error mapping and the toplevel's value printing; the biggest.
- **Lambda** (`-dlambda`): the compiler's IR after those translations: a small untyped lambda calculus with
  `Lfunction`, `Lapply`, `Lprim` (primitives, about 150), `Lswitch`, `Lstringswitch`, `Lstaticcatch`/
  `Lstaticraise` (compiled matches' exits), `Ltrywith`, `Lifthenelse`, `Lsequence`, `Lwhile`, `Lfor`,
  `Lassign`, `Lletrec`, `Levent`, plus `Lfunction`'s kind and arity, and (5.x) `Lmutlet`, `Lregion`. About 25 constructors
  and a *printer* (`-dlambda`), so a dump is a ready form; primitives and `Pmakeblock` tags and `Pfield` are
  exactly the runtime's data model; evaluation order explicit; `function` partial application
  handled (`curry`/`tupled` arity); modules are blocks of fields; objects are blocks plus method tables
  through `CamlinternalOO` (stdlib OCaml). **Semantics = Lambda's; types gone, so the toplevel's value
  printing needs the Typedtree's type of each phrase** (OC12: take it from the oracle).
- Flambda's IR is not available on this host (E2, flambda false).
- So: Lambda for eval and AOT, Typedtree dumped as annotations for the toplevel and for the error-judging
  (OC5 recommended (b)).
- A translation instead of an interpreter (OCaml → Clojure forms, say) is what whiml's neighbours did for
  the Scheme and is mismatched here: OCaml's semantics (curried partial application with arity, exceptions,
  mutable records, `==`) is not Clojure's.

### 6.4 The checker (if Arbace types, OC4 (b))

The checker of `goi` is "after go/types" (`arbace.go.check`, with the helper's annotations verified).
For OCaml, "after Typedtree" is **HM inference with levels, the module system (signatures, functors, applicative
paths, strengthening), recursive modules, GADT equations with local type constraints, polymorphic variants'
row variables, objects' row types, labelled/optional application, first-class modules, effects as an
open extensible type, and `Typecore`'s disambiguation**. `typing/` in the compiler is about 40 modules; a port is
the long pole (15-30 days with the oracle checking each program's types as go/types checks goi's). Without
it, Arbace takes the oracle's dumps for types: a program must be compiled by the host `ocamlc` to run in
oi, which stops short of "standalone", though the *record* of the dumps can be committed as data (the
recorded `.cmt`-style dump per corpus file, as goi records `gc`'s runs).

### 6.5 The REPL

`bin/oi`: phrases `let x = 1;;` and `# #use "f.ml";;` read by repr/ocaml, typed (by the oracle or checker), lowered
to Lambda (by the helper, or by an Arbace translator of the Typedtree, OC5), evaluated in a
toplevel environment (a module whose fields grow); directives (`#load`, `#require` etc.) are out
(a class).

## 7. AOT

Lambda → Java forms (a runtime library `jdk/ocaml/` of Java forms: `Block`, closures with arity, exceptions as
`OCamlException`) → j2g → Go, so Arbace's AOT pipeline (c2j, j2g, repr/go print, Go's compiler) is
reused; alternatively Lambda → Go forms directly (OC7). Effects need the same coroutine
mechanism as eval (OC6); proper tail calls need a trampoline in Java forms (Java and Go have none), with
self-tail-calls as loops, a known cost (a class: stack depth at 10^6). The reference is `ocamlopt`'s binary. The
cross-check, diffcheck's kind: oi against the compiled form.

## 8. The oracle and the corpora

### 8.1 The oracle

- **Helper `tools/ocdump`**: OCaml on `ocaml5-compiler-libs` 5.5.0, pinned by `tools/ocdump/OCAML_VERSION`
  (as `GHC_VERSION`), built by `ocamlopt -I +compiler-libs ocamlcommon.cmxa`. Dumps: Parsetree (`.ml`, `.mli`),
  tokens, comments, Typedtree (`Typemod.type_implementation`), Lambda (`Translmod.transl_implementation`), and the
  `.cmi`/`.cmt` digest. Needs `apk add ocaml5-compiler-libs` (70 MiB); `doc/ALPINE.md`'s minimal list
  gains it as a line (OC3).
- **A port of the parser instead**: `parser.mly` is Menhir; the committed `parser.ml` is generated; the
  compiler ships the generated parser as `boot/menhir/parser.ml` in the source tree (an OCaml file of
  about 40k lines using Menhir's table back end `--table`? The upstream uses the code back end), a
  port of that is translating OCaml to Arbace's forms, which is j2g-like in size. Its judge would still be
  the helper. So the helper is the oracle either way (OC3).
- References for runs: `ocamlopt` on the originals, recorded as data (`test/ocaml/ref-ocaml55.edn`).

### 8.2 The corpora

| corpus | source | license | use |
|---|---|---|---|
| the stdlib | ocaml/ocaml `stdlib/` at 5.5.0 (about 70 `.ml` + `.mli` pairs) | LGPL-2.1+ with linking exception | read, print; the runtime of eval (interpreted); AOT |
| the compiler's own sources | `parsing/`, `typing/`, `utils/`, `lambda/`, `driver/` | same | read, print (the biggest realistic `.ml`); `compiler-libs` is the same code |
| the testsuite | `testsuite/tests/` (ocamltest `.ml` files with `.reference`, `(* TEST ... *)` headers) | same | read; eval and AOT for tests with `.reference` and a plain `ocaml` or `ocamlopt` action |
| go-whim's whiml | `whiml/*.ml` (58,346 lines) | go-whim's license (Arbace's own pilot) | read, print, eval (a heavy case: 57k lines), AOT |
| opam packages (later) | a pinned set (e.g. `containers`, `fmt`, `astring`, `re`, `zarith` excluded for C stubs, `menhirLib`) via `opam` source tarballs | each package's (MIT, ISC, BSD, LGPL) | read, print; later corpus (OC10) |
| Arbace's own samples | `test/ocaml/samples/` (one file per feature of §4.2, plus effects, objects, GADTs) | Arbace's | all levels |

The testsuite is a *tests-of-the-compiler* corpus: many `.ml` are meant to **fail** typing or print a specific
error, or need ocamltest's actions (`flags`, `modules`, `ocamlopt.byte`, `native-compilation`), a few
thousand files; each gets a recorded action and a class (as `$GOROOT/test`'s are, GR9-GR10).

## 9. Licenses

`LICENSE.md` gains OCaml: the compiler and runtime `LGPL-2.1-or-later WITH OCaml-LGPL-linking-exception`
(the **linking exception**: the standard library and runtime may be linked statically into a program
without the program becoming LGPL), the stdlib the same, the testsuite the same (LGPL, no exception for
the tests), `compiler-libs` the same (linking the helper statically against it is allowed under the
exception). Vendoring the stdlib's `.ml` as OCaml forms is vendoring LGPL code: the notices stay, `LICENSE.md` states the
text and lists the files (OC13); whiml is go-whim's (Arbace's pilot). dune MIT, opam LGPL-2.1 with
exceptions, packages as each states. LGPL text for the bundled files is in the distribution.

## 10. The fifth row

`test/reap/cells.edn` gains `:ocaml` with four cells, the classes files
(`test/ocaml/divergences{,-read,-aot,-print}.edn`), `bin/reap` judging 20 of 20. The Makefile gains
`repr-ocaml`, `oi-check`. Parked bridges (repr/haskell, repr/rust) stay parked; OCaml is the first new crop
that needs a REPL and not only a bridge, so it lands as a crop (the rows' rule), not as a bridge.

## 11. Plan (O1-O9)

Estimates in working days as the repository's surveys give them. Each milestone ends with its check green
and its records.

| milestone | content | exit criteria | days |
|---|---|---|---:|
| **O1** the spec and pins | `doc/ocaml/SPEC.md`: OCaml forms for §4.2 (Parsetree of 5.5.0 complete: effects patterns, `let op`, `module type of`, `letop`, `include functor`), worked examples; pins (`tools/ocdump/OCAML_VERSION`, the source tarball's sha256, the opam set, `test/ocaml/corpora.edn`); the apk question settled; samples | the spec accepted by the user; the manifest lists every corpus with its pin; stdlib, testsuite counts measured (E3) | 2-3 |
| **O2** repr/ocaml `:lines` | `tools/ocdump` (Parsetree dump, with errors), `arbace.repr.ocaml.convert`, `.print` (`:lines`, layout engine), `bin/repr-ocaml read|print|roundtrip|check` | stdlib, compiler sources, samples read; forms → text → forms equal; the AST equal to the helper's on a re-parse of the print by `ocdump`; classes for helper-rejected files | 5-8 |
| **O3** `:full` | token and comment dump, mode `:full`, `check --text`; `.cmi` level | byte for byte over stdlib, compiler sources, whiml, testsuite's readable `.ml`; the `.cmi` bytes equal for the print | 3-5 |
| **O4** types and Lambda from the oracle | `ocdump` Typedtree and Lambda dumps as forms (`(lambda ...)`), types as annotations; or the checker (OC4 (b), +15-30) | Typedtree and Lambda dumps for every compilable corpus file; dump size and speed recorded | 4-6 |
| **O5** oi | `go/arbace/oi` (values, blocks, closures with arity, primitives table, exceptions, tail calls), the stdlib interpreted from Lambda, externals in Go, REPL `bin/oi`, `check`, `divergences`; refs recorded | `bin/oi check` over run corpora: none failing, every non-pass classed; a REPL session with value printing | 10-18 |
| **O5b** effects | OC6's mechanism; deep and shallow handlers; `Domain` and `Atomic`; the memory model classes | the testsuite's `effects/` and `parallel/` directories in results or classes | (in O5; 3-5 of its days) |
| **O6** AOT | Lambda → Java forms (runtime `jdk/ocaml/`) → j2g; shards built by `arbace.build.go` (under the lock); `check --aot` | every run program translated or refused with a reason; none failing; oi and compiled compared | 8-14 |
| **O6a** (optional) native lexer and parser | a port of `lexer.mll` and `parser.mly`'s grammar; `parse-check` against the helper, 8,000 mutants for error recovery | trees equal over every corpus | 8-15 |
| **O7** corpus: testsuite and opam | `testsuite/` actions and expected outputs classed; opam set pinned | each file in a result or a class | 3-5 |
| **O8** the fifth row | `cells.edn` `:ocaml`, classes, `bin/reap` 20 of 20, essential subsets in the gate, Makefile | `bin/reap`: 20 of 20 checked, none failing | 1-2 |
| **O9** the milestone | `bin/gate --full` serial with OCaml's checks, fixpoint, re-seed, tag | the tag | 1-2 |

**Total without O6a: about 37 to 63 working days** (O1 2-3, O2 5-8, O3 3-5, O4 4-6, O5 10-18, O6 8-14, O7 3-5, O8 1-2,
O9 1-2). With OC4 (b) (own checker) +15-30; with O6a +8-15; with both about 60 to 108. The long pole is O5, as
goi's GR3-GR10 and C5 were.

**Sonnet versus Opus** (per the user's model split): Sonnet: O1's spec drafting from the Parsetree's
definitions, O2's converter and printer once the head vocabulary is fixed, O3's token/comment weaving (mechanical
but fiddly), O7's action classing, O8, the Makefile and records; the dumper `ocdump` in OCaml, small and
mechanical. Opus: the design of oi's value model and tail calls, effects (O5b), the primitives table's
semantics (63-bit ints, float unboxing, polymorphic compare), O6's Java-forms runtime and trampolines,
OC4 (b)'s checker (modules, GADTs, row types), the divergence classes' judgment.

**Re-seed needs.** O1-O4 change only tools' namespaces (evaluated from the checkout) and a helper, no re-seed.
O5 adds `go/arbace/oi`, evaluated by goi until listed in goi's table and compiled (a re-seed, batched with C5/C6's if
those run). O6 changes jdk/ocaml and, if j2g needs variants, the program (a re-seed). One re-seed at O9 at
most if the table is kept for the end, as C3 did for C.

## 12. Proposals (OC1-OC16)

Each with options, the recommended one first.

- **OC1 The version pin and the name of the milestones.**
  - Options: (a) OCaml 5.5.0 as on the host and in go-whim's whiml (Alpine `ocaml5-5.5.0-r0`; upstream tag
    `5.5.0`, sha256 of the tarball and of the Alpine APKBUILD pinned at O1), milestones O1-O9 (note that O1-O9 do not
    clash with other records); (b) 5.4.x (a long-term release), needs a different install; (c) 4.14.4 (Alpine's
    `ocaml-4.14.4-r0`, the last without effects, a smaller language: no Domain, no effects, no 5.x
    Parsetree additions).
  - *Recommended: (a)*: it is the host's, whiml's, and a compiler built for effects; (c) would lose the
    hardest part and the one OCaml-specific feature.
- **OC2 Names: the interpreter and the milestone's tag.**
  - Options: (a) `oi`, tag `arbace-reaps-5-crops-v1` (counting C as the fourth, if C23 comes first; else
    `-4-crops`); (b) `ocaml-i`; (c) `mli`.
  - *Recommended: (a)* (the count follows the order, OC15).
- **OC3 The oracle: a compiler-libs helper or a port of the parser.**
  - Options: (a) helper `tools/ocdump` on `ocaml5-compiler-libs` 5.5.0 (apk, 70 MiB; recorded in `doc/ALPINE.md`
    and its apk pinned by a sha in the style of `tools/rsdump/apks.sha256`), a native parser (O6a) later to cut the
    dependency; (b) a native parser first (a port of `parser.mly`: no oracle for it on the first day,
    the helper is still needed to judge it); (c) `ocamlc -dparsetree`/`-dsource` text scraped, no apk
    (lossy, fragile, no tokens: only for a smoke test).
  - *Recommended: (a)*, the pattern of hsdump and rsdump: the oracle first, the native reader as a proved
    replacement later (GP5, JN8).
- **OC4 Types: inferred by Arbace or taken from the oracle.**
  - Options: (a) taken from the oracle in v1: `ocdump` dumps Typedtree and Lambda for each compilable file,
    committed as data per corpus (so the checks run with no OCaml on the host), Arbace's checker a later
    proposal; (b) an Arbace checker `arbace.ocaml.check` after Typedtree, +15-30 days, the oracle judging it
    as go/types judges goi's checker; (c) no types at all: eval on the Parsetree by name, wrong for
    disambiguation (§6.2).
  - *Recommended: (a)*: the checker is the largest single item (modules, GADTs, row types) and the value
    of the crop is already in read/print/eval/AOT; (b) as a follow-up when the user wants "standalone".
- **OC5 Eval's representation.**
  - Options: (a) interpret Lambda (dumped by the oracle; types for the toplevel from the Typedtree dump);
    (b) interpret the Typedtree (reimplement `Translcore`, `Matching`, `Translmod`, `Translclass`: +10-20 days);
    (c) translate to Clojure forms (semantics mismatch, §6.3).
  - *Recommended: (a)*: Lambda is exactly the data model and primitives; the order of evaluation and match
    compilation are done; (b) only with OC4 (b).
- **OC6 Effects.**
  - Options: (a) one-shot continuations by goroutine plus channel hand-off per handler (a fiber is a
    goroutine; `continue k v` resumes it; `discontinue` raises into it; a fiber stack never copied;
    `Effect.Deep` and `Shallow` over the same primitive); (b) CPS in the evaluator (every call a closure;
    slow and large); (c) out of scope for v1, recorded as a class (loses the testsuite's `effects/` and
    `Eio`-style programs, half the point of 5.x).
  - *Recommended: (a)*, with (c) as the fallback if O5's design shows a problem; AOT uses the same.
- **OC7 AOT's path.**
  - Options: (a) Lambda → Java forms (a runtime `jdk/ocaml/` in Java forms) → j2g → Go; (b) Lambda → Go forms
    directly (a runtime in Go forms, no j2g); (c) via Arbace's own c2j by first translating OCaml to Arbace
    (mismatch).
  - *Recommended: (a)*: Arbace's pipeline and optimizations are j2g's and the Java forms are the pivot, as
    Clojure's AOT is; (b) if (a)'s Java forms prove awkward for curried closures and tail calls.
- **OC8 The forms' vocabulary.**
  - Options: (a) the Parsetree's constructors as heads (near-identity with the dump), with Go forms' conventions
    for lists, names and flags; (b) a desugared core (Lambda-like) as the user-facing form (loses the round
    trip); (c) OCaml's own concrete syntax embedded in strings (no).
  - *Recommended: (a)*, as repr/haskell's forms mirror GHC's `HsSyn`.
- **OC9 PPX and extensions.**
  - Options: (a) forms keep attributes and extensions as written (pre-PPX); a PPX-expanded tree is not read in v1;
    corpus files that need a PPX (`[@@deriving]` with no expansion available) are read but judged
    only at read/print, a class at eval; (b) a PPX host (ppxlib in OCaml) driven from the helper (a big
    dependency); (c) an Arbace-side `deriving show/eq/ord` for the common ones.
  - *Recommended: (a)*, (c) later by evidence.
- **OC10 The corpus.**
  - Options: (a) stdlib, compiler's sources, testsuite, whiml, samples at O1-O7; opam later; (b) add an opam
    set at O1 (pins, many packages need C stubs or dune); (c) only the stdlib and samples.
  - *Recommended: (a)*, the opam set at O7 with a small pinned list of pure-OCaml packages.
- **OC11 Divergence classes by design.**
  - Options: (a) `Obj.*` internals, `Gc.*` statistics and finalisation order, `Weak`/`Ephemeron`, the memory-model
    races, byte-exact `Printexc` backtraces (the line numbers from `-g`), `Sys.word_size` as 64 and int as 63 bits kept,
    `Unix` and `Str` C-stub libraries as listed, stack overflow depth; (b) fewer; (c) none (impossible).
  - *Recommended: (a)*, each class citing where it was decided.
- **OC12 The toplevel's printing.**
  - Options: (a) the oracle gives each phrase's type; value printing is oi's own `Genprintval` over a block and its type; (b)
    no toplevel printing (programs only); (c) the oracle prints.
  - *Recommended: (a)*, (b) if OC4 (a)'s dumps make per-phrase types awkward.
- **OC13 Vendoring the stdlib.**
  - Options: (a) stdlib's `.ml` read as OCaml forms (`ocaml/stdlib/`, committed like `jdk/`, `tamago/goroot`), a `bin/ocaml-vendor
    convert|verify` tool, LGPL notices in `LICENSE.md`; (b) read from the pinned tarball at run (extracted into `.tmp/`);
    (c) the host's `.cmx` files (no sources).
  - *Recommended: (a)*, the repository's way (`LICENSE.md` extended).
- **OC14 The native parser.**
  - Options: (a) O6a after the milestone, if the apk dependency is a problem; (b) in the milestone; (c) never.
  - *Recommended: (a)*.
- **OC15 Order relative to C23, net/http and the Scheme crop.**
  - Options: (a) C23 first (its survey is accepted and under way; it needs gcc only), then OCaml, net/http
    (a library and a server, a different kind of work: no new language) beside either, Scheme last (smallest: R7RS, an
    s-expression language that needs no repr and little eval; but go-whim's `togo/scm*.go` shows it already
    exists as a target) — OCaml before Scheme if the user wants effects; (b) OCaml first (its helper pattern
    is shorter than C's preprocessor work, and its oracle is on the host); (c) Scheme first (cheapest: a small reader and
    an evaluator, perhaps 10-15 days).
  - *Recommended: (a)*: finish what is surveyed; Scheme is the cheap filler between big crops; net/http is not a row.
    (This survey did not read the net/http or Scheme surveys; the user's statement that a Scheme survey exists
    "beside this" is taken as given: coordinate their shared items, e.g. a delimited-continuation mechanism,
    which Scheme's `call/cc` and OCaml's effects both need, OC6 and Scheme's.)
- **OC16 How to split the work between models.**
  - Options: (a) O1-O4 by Sonnet in parallel (spec, helper, converter, printer), O5-O6 by Opus; (b) all Opus; (c) all Sonnet.
  - *Recommended: (a)*, per §11.

## 13. Order and what to decide first

First: OC1 (pin), OC3 (the apk, the oracle), OC4 (types), OC5 (eval's representation), OC6 (effects), then OC15 (when).
The rest follow from those. If the user wants a cheap trial, O1+O2 (7-11 days) give a read/print round trip with
no runtime commitment.

## 14. Risks

- **The apk.** The helper needs `ocaml5-compiler-libs` (70 MiB), not installed; Alpine edge moves (5.5.0 is
  pinned by the package's `-r0` and by sha, and the next edge may drop it). Mitigation: pin the apk's sha256,
  O6a.
- **Lossy trees** (E1): `:full` is more work than the tree level; comments attach by position, and the compiler's
  parser inserts ghost nodes. The risk is days on ghost locations; mitigation: the token stream.
- **Type-dependence of semantics**: with OC4 (a) every eval corpus file must be compilable by `ocamlc` to be
  dumped, and error tests judged by message cannot be eval'd; the dumps are data and large (Typedtree of the
  stdlib, megabytes).
- **Effects on a Go-hosted runtime**: goroutine switches are costly (about 200 ns to microseconds), deep recursion inside a
  handler needs a big stack; the model of 5.x's memory (races) cannot be matched.
- **Number semantics**: 63-bit ints and unboxed floats, `Int64`/`Nativeint` boxed; subtle, testable.
- **Stdlib sources not on the host**: sizes and the testsuite's count are unmeasured here; an estimate gap in O7.
- **Testsuite is the compiler's own**: many tests exercise the toplevel, ocamltest actions, `-dparsetree`'s text, or
  expected *type errors*; the fraction that is a plain run program is unknown (guess 30-50%), so a large class file.
- **Estimates run optimistic or not**: the long pole O5's range is wide (10-18) because the primitives table's size
  is unmeasured; re-estimated after O4.
- **OCaml-LGPL**: vendoring the stdlib puts LGPL text in the tree; the exception covers linking the runtime, not
  every redistribution; `LICENSE.md` must be exact (OC13).
- **Menhir was not checked for on the host**: a port of `parser.mly` needs the generated parser from
  the compiler source (`boot/menhir/parser.ml`), whose license is the compiler's.

## 15. Sources

- The host: `/usr/bin/ocaml`, `ocamlc`, `ocamlopt` 5.5.0 (`ocamlopt -config`: architecture amd64, flambda false);
  apk `ocaml5-5.5.0-r0`, `ocaml5-runtime-5.5.0-r0` (installed), `ocaml5-compiler-libs-5.5.0-r0` (70 MiB, available, not
  installed), `dune-3.22.2-r2` (MIT); licenses `LGPL-2.1-or-later WITH OCaml-LGPL-linking-exception` as `apk list` prints.
- Upstream: `https://github.com/ocaml/ocaml`, tag `5.5.0` (not downloaded: pin and sha256 at O1); directories
  `parsing/` (`parsetree.mli`, `lexer.mll`, `parser.mly`, `pprintast.ml`), `typing/` (`typedtree.mli`, `typecore.ml`,
  `typemod.ml`), `lambda/` (`lambda.mli`, `translcore.ml`, `matching.ml`, `printlambda.ml`), `runtime/`, `stdlib/`,
  `testsuite/`, `ocamltest/`; the manual (`ocaml5-doc`). Alpine aports, `main/ocaml5/APKBUILD` (tarball name and sha512).
- go-whim `/root/go-whim` (the checkout; the revision `80c49bf5` was named by C23-SURVEY, not re-read here): `whiml/`
  (editor.ml, rt.ml, host.ml, term.ml, snprintf.ml, main.ml), `doc/OCAML.md`, `doc/OCAML-IDIOMS.md`,
  `crefactor/togo/ml*.go`; `doc/SCHEME.md`, `doc/GHC-LISP.md`.
- Arbace: CLAUDE.md; doc/FULL-SUPPORT-SURVEY.md (the matrix); doc/C23-SURVEY.md (this file's shape);
  doc/haskell/SPEC.md and HASKELL-NOTES.md, doc/rust/SPEC.md (the helper pattern); doc/go/GOI-SPEC.md (the
  interpreter's design); `tools/hsdump`, `tools/rsdump`.
- Probes: `.tmp/ocaml/t.ml`, `u.ml`, `e.ml` in the worktree `.tmp/worktrees/ocaml-survey` (scratch, gitignored).
