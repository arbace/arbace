# OCaml forms: OCaml modules written in Clojure

Status: draft for the user's review, 2026-10-30: milestone O1 of the OCaml crop
([OCAML-SURVEY.md](OCAML-SURVEY.md) §11, "As built: O1"), bound by the survey's decisions OC1-OC16 (its section
"Decided (2026-10-30)"). The choices made while writing it are S1-S29 (§13.1); the open questions are the
proposals OC20 onward of the survey. Nothing in it is a shipped file; the helper `tools/ocdump`, the script
`bin/repr-ocaml` and the data of `test/ocaml/` are the first code of the crop.

This spec defines the **OCaml forms**: how every construct of an OCaml 5.5.0 module, as the compiler's
parser sees it (`Parsetree`, `parsing/parsetree.mli` of the release tarball pinned in
`test/ocaml/corpora.edn`), is written as Clojure data in ordinary `.ae` files that Arbace's reader reads, what
the forms keep of the source, and how the round trip OCaml → forms → OCaml is to be judged. It is the fifth
bridge of the `repr` family after repr/java, repr/go, repr/haskell and repr/rust, and follows repr/go's and
repr/haskell's specs in structure and conventions ([../go/SPEC.md](../go/SPEC.md),
[../haskell/SPEC.md](../haskell/SPEC.md)).

Words used throughout:

- **the helper**: `tools/ocdump/ocdump.ml`, an OCaml program on compiler-libs (`ocamlcommon`), built by the
  host's `ocamlopt`. It parses one file with the compiler's own parser (`Parse.implementation`,
  `Parse.interface`, `Parse.use_file`) and writes the tree **in the forms of this spec**: the vocabulary is the
  Parsetree's (OC8 (a)), so the converter of O2 is near the identity and the helper decides nothing the
  spec does not. Its other modes (§11) dump the typed annotations and Lambda (OC4 (a)) and the tokens.
- **the converter** (O2): `arbace.repr.ocaml.convert`, in Arbace, which reads the helper's output and writes
  `.ae` files; **the printer** (O2): `arbace.repr.ocaml.print`, forms → OCaml text.
- **a form**: a list `(head field...)`, optionally ending in one metadata map (§1.3).
- **OCaml context**: the inside of a top-level form (`ocaml/impl`, `ocaml/intf`, ...), where the forms mean OCaml.
- **the checklist**: §12, every constructor of `parsetree.mli` and `asttypes.mli` with its form; checked
  against the tarball's files by `bin/repr-ocaml spec-check`.

Contents: 1 Principles · 2 Names, labels, flags and constants · 3 Types · 4 Patterns · 5 Expressions ·
6 Classes · 7 Modules · 8 Items, attributes and extensions · 9 Toplevel phrases and errors · 10 Positions ·
11 The helper's other modes · 12 The checklist · 13 Decisions and open questions · 14 The round trip and the
checks · 15 Sources · Appendix A: the experiments.

## 1. Principles

### 1.1 What the forms are

The forms follow the compiler's Parsetree one node to one list. The tree is **lossy** (the survey's probe E1):
comments, parenthesization, `begin`/`end` against parentheses, `;;`, the spelling of `fun`/`function` sugar
and whitespace are gone, and docstrings survive only as attributes. The forms keep exactly what the Parsetree
keeps, in mode `:lines`; mode `:full` (O3, the text byte for byte) adds the token stream the helper dumps (§11.3)
and is not defined here beyond that (§14.2). Literals keep their source text (`0x1F`, `1_000`, `3.50`,
`{id|...|id}`), because the Parsetree does (S6).

What the Parsetree has and the forms keep: the sugar the parser leaves (`fun x y -> e` is one `function` node
with two params; a list `[1; 2]` is `::` constructors; `if` without `else` has no third field; `a.(i)` is an
application of `Array.get`: the parser desugars indexing, so it is `(apply (ident Array.get) ...)`, S9), the
ghost locations of invented nodes (§10), the attributes and extension points as written (pre-PPX, OC9 (a)).

### 1.2 Conventions

- **Heads** are the Parsetree constructor's name without its prefix, lower-cased, `_` as `-`: `Pexp_apply` is
  `apply`, `Pexp_ifthenelse` is `ifthenelse`, `Pexp_struct_item` is `struct-item`. Records without a constructor
  get a head from the record (`case`, `binding`, `decl`, `cdecl`, `ldecl`, `param`, `bop`, ...), listed in §12.
  The same head may stand in different categories (`ident` is an expression, a module type and a module
  expression, `constr` a type, a class type and a class expression): **the slot decides the category**,
  as in Go forms. No head is a Clojure special form's name in a position where it would be evaluated: OCaml forms
  are data.
- **Fields** follow the Parsetree's, in its order, except that the node's attributes and positions go to the
  metadata (§1.3).
- **Lists** (OCaml lists, `list` fields) are vectors `[a b c]`. A **pair or triple** of a list's element
  (`(label * expression)`, `(longident * pattern)`, a type parameter with its variance) is a vector
  `[label expr]`, so a vector in a list is a tuple of that node's documented shape.
- **Options**: `None` is `nil` in the middle of a form, and a *trailing* `None` is left out (S2): `(construct :none)`
  has no argument, `(construct Some (ident x))` has one. The checklist marks each trailing option with `?`.
  Where an option is not trailing (`pexp_coerce`'s `from`, a `param`'s default) the slot holds `nil`.
- **Flags** are keywords: `:rec` `:nonrec`, `:to` `:downto`, `:private` `:public`, `:mutable` `:immutable`,
  `:virtual` `:concrete`, `:override` `:fresh`, `:closed` `:open`; variance `:+` `:-` `:+-` (or `nil`),
  injectivity `:!` (or `nil`) (§2.4).
- **Strings** that are source text are Clojure strings, with Clojure's escapes; bytes that are not UTF-8 make
  the constant a vector of byte values (§2.5).
- **Booleans** are `true` and `false`.

### 1.3 The metadata map

A node may end in **one map** (and no field is a map), with these keys, each present only when it has a value:

| key | value | when |
|---|---|---|
| `:attrs` | a vector of `attr` forms (§8.1) | the node has attributes (`[@a]`, `[@@a]`) |
| `:loc` | `[start-line start-col end-line end-col start-offset end-offset]` | positions are on (§10) |
| `:ghost` | `true` | the location is a ghost (the parser invented the node) |
| `:stack` | a vector of locs | positions are on and the node has a `loc_stack` (types, patterns, expressions) |
| `:content-loc` | a loc | a string constant, positions on: the span of the content without the delimiters |

The top-level forms' map carries `:file` and `:ocaml` (§8.5). The reader of forms ignores keys it does not know.

### 1.4 What a consumer may rely on

A consumer matches on the head and the field count; the fields are positional. Every head of §12 has a fixed
arity modulo its trailing options. Unknown heads are an error, not an extension point (S5): a new compiler
version is a new spec (OC1).

## 2. Names, labels, flags and constants

### 2.1 Names

A name is written **bare** (a symbol) exactly when it is an OCaml identifier that Arbace's reader reads back as
the same symbol:

- its text matches `[A-Za-z_][A-Za-z0-9_']*` and is not `true`, `false` or `nil`.

Every other name is `(name "text")`: the operators (`+`, `|>`, `**`, `:=`, `!`, `~-`), the binding operators (`let*`),
the indexing operators (`.%()`, `.%()<-`), `true`, `false`, `nil` and names with non-ASCII letters (S3). The word
operators `mod`, `land`, `lor`, `lxor`, `lsl`, `lsr`, `asr` and `or` have the shape of identifiers and are bare. The helper applies the rule to every name; the converter checks it by reading what it wrote.
The cases the reader refuses (`:=`, `::`, `^`, `@`, `~-`, `!`, `#`, `.%`, `<-`...) are all in the second class,
and the rule never writes a symbol it has to guess.

Built-in constructors, which are `Lident`s of special text in the Parsetree, are keywords (S4):

| OCaml | form |
|---|---|
| `()` | `:unit` |
| `[]` | `:nil` |
| `::` | `:cons` |
| `true`, `false` | `:true`, `:false` |

so `1 :: xs` is `(construct :cons (tuple [(constant (int "1")) (ident xs)]))`, as the parser has it (one tuple
argument, §5.3).

### 2.2 Long identifiers

`Longident.t` is `Lident | Ldot | Lapply`. A path without functor application is **one symbol with its dots**
when every component is bare (`List.map`, `Stdlib.Map.Make`, `M.x`), else `(name "M.+")` with the whole path
as text: the components but the last are module names, so the text splits unambiguously (after the leading
`Uident.` components, the rest is the last component). A path with a functor application inside uses
`(ldot PATH NAME)` and `(lapply PATH PATH)`: `F(X).t` is `(ldot (lapply F X) t)`. Long identifiers appear in `ident`,
`construct`, `field`, `record`, `new`, `open`, `constr`, `class`, `type`, module paths, `with` constraints.

### 2.3 Labels

An argument label (`arg_label`) is `nil` for `Nolabel`, the keyword `:l` for `~l:` (Labelled `"l"`) and the
keyword `:?l` for `?l:` (Optional `"l"`). In a tuple (OCaml 5.4 labelled tuples), an unlabelled element is
the element's form and a labelled one `[:l form]`; types, patterns and expressions alike.

### 2.4 Variance and injectivity

A type parameter (in `decl`, `typext`, `class-infos`) is `[TYPE VARIANCE INJECTIVITY]`, with `nil`s when absent:
`+'a` is `[(var a) :+ nil]`, `!'a` is `[(var a) nil :!]`, `-'a` `:-`, `+-'a` is Bivariant, `:+-`.

### 2.5 Constants

The Parsetree's `constant` record becomes one of

| form | OCaml | notes |
|---|---|---|
| `(int "text" [SUFFIX])` | `3`, `0x1F`, `1_000`, `3l`, `3L`, `3n` | text as written, an optional suffix as the character code (`108` for `l`); the parser accepts suffixes `[g-z][G-Z]` and the typechecker rejects the others |
| `(char CODE)` | `'a'`, `'\n'`, `'\255'` | the byte value |
| `(string STR [DELIM])` | `"s"`, `{id|s|id}` | the value (escapes decoded); `DELIM` is the quoted string's id (`""` for `{\|s\|}`) |
| `(float "text" [SUFFIX])` | `3.4`, `2e5`, `1.4e-4`, `0x1p3` | text as written |

A string whose bytes are not valid UTF-8 is `(string [b0 b1 ...])` with the byte values. With positions on, the
constant has `:loc` and a string constant `:content-loc`.

Examples (each of the examples of this spec is checked against the helper, §14.1):

```forms
;; expr: 0x1F
(constant (int "0x1F"))
;; expr: 3L
(constant (int "3" 76))
;; expr: 1_000.5e3
(constant (float "1_000.5e3"))
;; expr: 'a'
(constant (char 97))
;; expr: '\255'
(constant (char 255))
;; expr: "tab\there"
(constant (string "tab\there"))
;; expr: {id|a "raw" string|id}
(constant (string "a \"raw\" string" "id"))
;; expr: "\xff\xfe"
(constant (string [255 254]))
;; pat: 'a' .. 'z'
(interval (char 97) (char 122))
```

## 3. Types

`core_type` has the fields of the Parsetree (the `ptyp_desc` becomes the head and fields, `ptyp_attributes` the
`:attrs` metadata, `ptyp_loc` and `ptyp_loc_stack` the `:loc` and `:stack`).

| head | OCaml | fields |
|---|---|---|
| `(any)` | `_` | |
| `(var NAME)` | `'a` | the name without the quote |
| `(arrow LABEL T1 T2)` | `T1 -> T2`, `l:T1 -> T2`, `?l:T1 -> T2` | |
| `(tuple [ELEM...])` | `T1 * T2`, `l:T1 * T2` | elements as §2.3 |
| `(constr LID [T...])` | `int`, `'a list`, `(int, string) result` | |
| `(object [FIELD...] FLAG)` | `< m : t; .. >` | `:closed` / `:open` (`..`) |
| `(class LID [T...])` | `#c`, `'a #c` | |
| `(alias T NAME)` | `T as 'a` | |
| `(variant [ROW...] FLAG LABELS)` | `` [ `A \| `B ] ``, `[> ...]`, `[< ... > ...]` | `LABELS` is `nil` or a vector of the `>` tags |
| `(poly [NAME...] T)` | `'a 'b. T` | |
| `(package PKG)` | `(module S with type t = T)` | `PKG` as below |
| `(open LID T)` | `M.(T)` | |
| `(extension EXT)` | `[%id]` | §8.2 |
| `(functor LABEL NAME PKG T)` | `(module M : S) -> T` | OCaml 5.5's module-dependent arrow (modular explicits) |

`PKG` is `(pkg LID [[LID T]...])` (`ppt_path`, `ppt_constraints`, with attributes and positions).
A variant type's rows are `(rtag NAME BOOL [T...])` (`` `A of T1 & T2 ``; the boolean is the constant
constructor flag) and `(rinherit T)`; an object type's fields are `(tag NAME T)` and `(inherit T)`. The
variant's `LABELS` follow the Parsetree: `nil` for `[ ... ]` and `[> ... ]` (the `FLAG` tells), `[]` for
`[< ... ]`, the names after `>` otherwise.

```forms
;; type: 'a
(var a)
;; type: int -> ?opt:int -> lbl:string -> unit
(arrow
 nil
 (constr int [])
 (arrow :?opt (constr int []) (arrow :lbl (constr string []) (constr unit []))))
;; type: x:int * string
(tuple [[:x (constr int [])] (constr string [])])
;; type: (int, string) result
(constr result [(constr int []) (constr string [])])
;; type: < m : int; n : 'a -> unit; .. >
(object [(tag m (constr int [])) (tag n (arrow nil (var a) (constr unit [])))] :open)
;; type: 'a #c
(class c [(var a)])
;; type: (int * int) as 'x
(alias (tuple [(constr int []) (constr int [])]) x)
;; type: [ `A | `B of int | `C of int & string ]
(variant
 [(rtag A true [])
  (rtag B false [(constr int [])])
  (rtag C false [(constr int []) (constr string [])])]
 :closed
 nil)
;; type: [> `A | `B ]
(variant [(rtag A true []) (rtag B true [])] :open nil)
;; type: [< `A | `B > `A ]
(variant [(rtag A true []) (rtag B true [])] :closed [A])
;; type: [ pv | `D ]
(variant [(rinherit (constr pv [])) (rtag D true [])] :closed nil)
;; type: (module Set.S with type elt = int)
(package (pkg Set.S [[elt (constr int [])]]))
;; type: M.(t)
(open M (constr t []))
;; type: (module M : S) -> M.t
(functor nil M (pkg S []) (constr M.t []))
;; type: [%ty]
(extension (ext ty (str [])))
```

## 4. Patterns

| head | OCaml | notes |
|---|---|---|
| `(any)` | `_` | |
| `(var NAME)` | `x` | |
| `(alias P NAME)` | `P as x` | |
| `(constant C)` | `1`, `'a'`, `"s"`, `1.0` | |
| `(interval C1 C2)` | `'a'..'z'` | |
| `(tuple [ELEM...] FLAG)` | `(p1, p2)`, `(~l:p, q)`, `(p, ..)` | `:closed`, or `:open` for the trailing `..` |
| `(construct LID [VARS P])` | `C`, `C p`, `C (type a b) p` | no argument: nothing after `LID`; `VARS` the locally abstract types (`[]` usually) |
| `(variant NAME [P])` | `` `A ``, `` `A p `` | |
| `(record [[LID P]...] FLAG)` | `{ l = p; _ }` | `:open` for `_` |
| `(array [P...])` | `[\| p1; p2 \|]` | |
| `(or P1 P2)` | `p1 \| p2` | |
| `(constraint P T)` | `(p : t)` | |
| `(type LID)` | `#t` | |
| `(lazy P)` | `lazy p` | |
| `(unpack NAME [PKG])` | `(module M)`, `(module M : S)`, `(module _)` | `NAME` is `nil` for `_` |
| `(exception P)` | `exception p` | in a `match` case |
| `(effect P1 P2)` | `effect p1, k` | OCaml 5.3's effect case: the effect pattern and the continuation's |
| `(extension EXT)` | `[%id]` | |
| `(open LID P)` | `M.(p)` | |

```forms
;; pat: (a, b)
(tuple [(var a) (var b)] :closed)
;; pat: (~l:a, b)
(tuple [[:l (var a)] (var b)] :closed)
;; pat: (a, ..)
(tuple [(var a)] :open)
;; pat: Some (x, _)
(construct Some [] (tuple [(var x) (any)] :closed))
;; pat: C (type a b) x
(construct C [a b] (var x))
;; pat: `A
(variant A)
;; pat: `B (x, y)
(variant B (tuple [(var x) (var y)] :closed))
;; pat: { l1 = p; l2; _ }
(record [[l1 (var p)] [l2 (var l2)]] :open)
;; pat: [| a; b |]
(array [(var a) (var b)])
;; pat: (A | B) as x
(alias (or (construct A) (construct B)) x)
;; pat: (x : int)
(constraint (var x) (constr int []))
;; pat: #t
(type t)
;; pat: lazy x
(lazy (var x))
;; pat: (module M : S)
(unpack M (pkg S []))
;; pat: (module _)
(unpack nil)
;; pat: x :: rest
(construct :cons [] (tuple [(var x) (var rest)] :closed))
;; pat: M.(x)
(open M (var x))
```

`exception` and `effect` patterns occur only as the left side of a `match` case (a `case` of §5):

```forms
;; expr: match f () with v -> v | exception Not_found -> 0 | effect Ask, k -> continue k 7
(match
 (apply (ident f) [[nil (construct :unit)]])
 [(case (var v) nil (ident v))
  (case (exception (construct Not_found)) nil (constant (int "0")))
  (case
   (effect (construct Ask) (var k))
   nil
   (apply (ident continue) [[nil (ident k)] [nil (constant (int "7"))]]))])
```

## 5. Expressions

| head | OCaml | fields |
|---|---|---|
| `(ident LID)` | `x`, `M.x`, `(+)` | |
| `(constant C)` | `1` | |
| `(let FLAG [BINDING...] BODY)` | `let p = e in b`, `let rec ... and ...` | `BINDING` is `(binding P E [VC])` |
| `(function [PARAM...] CONSTRAINT BODY)` | `fun p1 p2 -> e`, `function ...`, `fun (type a) -> e` | §5.1 |
| `(apply F [[LABEL E]...])` | `f a ~l:b ?o:c` | |
| `(match E [CASE...])` | `match e with ...` | `CASE` is `(case P GUARD RHS)`, `GUARD` `nil` or an expression |
| `(try E [CASE...])` | `try e with ...` | |
| `(tuple [ELEM...])` | `(a, b)`, `(~l:a, b)` | §2.3 |
| `(construct LID [E])` | `C`, `C e`, `C (a, b)` | the argument is one expression (a `tuple` for several) |
| `(variant NAME [E])` | `` `A ``, `` `A e `` | |
| `(record [[LID E]...] [BASE])` | `{ l = e; ... }`, `{ b with l = e }` | |
| `(field E LID)` | `e.l` | |
| `(setfield E LID E)` | `e.l <- e` | |
| `(array [E...])` | `[\| a; b \|]` | |
| `(ifthenelse C T [E])` | `if c then t else e` | |
| `(sequence A B)` | `a; b` | |
| `(while C B)` | `while c do b done` | |
| `(for P LO HI DIR BODY)` | `for i = lo to hi do b done` | `DIR` `:to` / `:downto`; `P` is a `var` or `any` pattern |
| `(constraint E T)` | `(e : t)` | |
| `(coerce E FROM TO)` | `(e :> t)`, `(e : t0 :> t)` | `FROM` `nil` for the first |
| `(send E NAME)` | `e#m` | |
| `(new LID)` | `new c` | |
| `(setinstvar NAME E)` | `x <- e` | |
| `(override [[NAME E]...])` | `{< x = e; ... >}` | |
| `(struct-item ITEM E)` | `let open M in e`, `M.(e)`, `let module M = ME in e`, `let exception E in e` | a structure item in expression position (OCaml 5.5) |
| `(assert E)` | `assert e` | |
| `(lazy E)` | `lazy e` | |
| `(poly E [T])` | a method body | only under `(concrete ...)` in a class (§6) |
| `(object CSTRUCT)` | `object ... end` | §6 |
| `(newtype NAME E)` | `fun (type t) -> e` as an expression | the form the parser gives a `(type t)` it does not fold into `function` |
| `(pack ME [PKG])` | `(module ME)`, `(module ME : S)` | |
| `(letop BOP [BOP...] BODY)` | `let* p = e and* q = f in b` | `BOP` is `(bop NAME P E)` |
| `(extension EXT)` | `[%id]` | |
| `(unreachable)` | `.` | the refutation case |

### 5.1 Functions

OCaml 5.2's Parsetree has one node, `Pexp_function (params, constraint, body)`, for `fun` and `function`. It
becomes `(function [PARAM...] CONSTRAINT BODY)`:

- `PARAM` is `(param LABEL DEFAULT P)` (`Pparam_val`: `P`, `~l:P`, `?l:P`, `?l:(P = E)`) or `(newtype NAME)`
  (`Pparam_newtype`; `(type a b)` is two nodes, the second with a ghost location);
- `CONSTRAINT` is `nil`, `(constraint T)` (`fun p : t -> e`) or `(coerce FROM TO)` (`fun p :> t -> e`);
- `BODY` is `(body E)` or `(cases [CASE...])` (`Pfunction_cases`; its location and attributes are the node's
  metadata).

The parser folds `fun x -> function A -> 1 | B -> 2` into **one** `function` node with the params `[x]` and the
body `(cases ...)`; a plain `function A -> 1` is the same node with no params (`[]`) and `(cases ...)`.

```forms
;; expr: fun x y -> x
(function [(param nil nil (var x)) (param nil nil (var y))] nil (body (ident x)))
;; expr: fun ?(x = 1) ~y (A z) -> x
(function
 [(param :?x (constant (int "1")) (var x))
  (param :y nil (var y))
  (param nil nil (construct A [] (var z)))]
 nil
 (body (ident x)))
;; expr: fun (type a) (x : a) -> x
(function [(newtype a) (param nil nil (constraint (var x) (constr a [])))] nil (body (ident x)))
;; expr: function None -> 0 | Some x -> x
(function
 []
 nil
 (cases
  [(case (construct None) nil (constant (int "0")))
   (case (construct Some [] (var x)) nil (ident x))]))
;; expr: fun x -> function A -> 1 | B -> 2
(function
 [(param nil nil (var x))]
 nil
 (cases
  [(case (construct A) nil (constant (int "1")))
   (case (construct B) nil (constant (int "2")))]))
;; expr: fun x : int -> x
(function [(param nil nil (var x))] (constraint (constr int [])) (body (ident x)))
```

### 5.2 Applications, operators and sugar

An application is `(apply F [[LABEL ARG]...])`; labels per §2.3, `nil` for an unlabelled argument. **Operators are
identifiers**: `1 + 2` is `(apply (ident (name "+")) [[nil 1] [nil 2]])`; `-x` is `(apply (ident (name "~-")) ...)`;
`x |> f` an application of `|>`; `&&` and `||` applications too (the parser makes no special node, S8). Indexing
`a.(i)` is `Array.get` applied (S9); `s.[i]` is `String.get`; `a.%(i)` (a user indexing operator) is the
application of the identifier `.%()`; their assignments `a.(i) <- v` are `Array.set` applications, and
`r.f <- v` is `setfield`.

```forms
;; expr: 1 + 2 * 3
(apply
 (ident (name "+"))
 [[nil (constant (int "1"))]
  [nil (apply (ident (name "*")) [[nil (constant (int "2"))] [nil (constant (int "3"))]])]])
;; expr: f ~x:1 ?y:(Some 2) 3
(apply
 (ident f)
 [[:x (constant (int "1"))]
  [:?y (construct Some (constant (int "2")))]
  [nil (constant (int "3"))]])
;; expr: f ~x ?y
(apply (ident f) [[:x (ident x)] [:?y (ident y)]])
;; expr: x |> f |> g
(apply
 (ident (name "|>"))
 [[nil (apply (ident (name "|>")) [[nil (ident x)] [nil (ident f)]])] [nil (ident g)]])
;; expr: a && b || c
(apply
 (ident (name "||"))
 [[nil (apply (ident (name "&&")) [[nil (ident a)] [nil (ident b)]])] [nil (ident c)]])
;; expr: -1
(constant (int "-1"))
;; expr: - x
(apply (ident (name "~-")) [[nil (ident x)]])
;; expr: not x
(apply (ident not) [[nil (ident x)]])
;; expr: a.(0)
(apply (ident Array.get) [[nil (ident a)] [nil (constant (int "0"))]])
;; expr: a.(0) <- 1
(apply
 (ident Array.set)
 [[nil (ident a)] [nil (constant (int "0"))] [nil (constant (int "1"))]])
;; expr: s.[0]
(apply (ident String.get) [[nil (ident s)] [nil (constant (int "0"))]])
;; expr: a.%(0)
(apply (ident (name ".%()")) [[nil (ident a)] [nil (constant (int "0"))]])
;; expr: a.%(0) <- 1
(apply
 (ident (name ".%()<-"))
 [[nil (ident a)] [nil (constant (int "0"))] [nil (constant (int "1"))]])
;; expr: r.f.g
(field (field (ident r) f) g)
;; expr: r.f <- 1
(setfield (ident r) f (constant (int "1")))
;; expr: !x := 2
(apply
 (ident (name ":="))
 [[nil (apply (ident (name "!")) [[nil (ident x)]])] [nil (constant (int "2"))]])
;; expr: (+)
(ident (name "+"))
;; expr: Stdlib.( + )
(ident (name "Stdlib.+"))
;; expr: List.( @ )
(ident (name "List.@"))
```

### 5.3 Data

```forms
;; expr: (1, "two")
(tuple [(constant (int "1")) (constant (string "two"))])
;; expr: (~a:1, b)
(tuple [[:a (constant (int "1"))] (ident b)])
;; expr: [1; 2; 3]
(construct
 :cons
 (tuple
  [(constant (int "1"))
   (construct
    :cons
    (tuple
     [(constant (int "2"))
      (construct :cons (tuple [(constant (int "3")) (construct :nil)]))]))]))
;; expr: 1 :: xs
(construct :cons (tuple [(constant (int "1")) (ident xs)]))
;; expr: Some (1, 2)
(construct Some (tuple [(constant (int "1")) (constant (int "2"))]))
;; expr: ()
(construct :unit)
;; expr: [| 1; 2 |]
(array [(constant (int "1")) (constant (int "2"))])
;; expr: `A
(variant A)
;; expr: `B 1
(variant B (constant (int "1")))
;; expr: { x = 1; y }
(record [[x (constant (int "1"))] [y (ident y)]])
;; expr: { r with x = 2 }
(record [[x (constant (int "2"))]] (ident r))
;; expr: r.x
(field (ident r) x)
;; expr: true
(construct :true)
;; expr: M.C 1
(construct M.C (constant (int "1")))
```

### 5.4 Control

```forms
;; expr: if c then a
(ifthenelse (ident c) (ident a))
;; expr: if c then a else b
(ifthenelse (ident c) (ident a) (ident b))
;; expr: a; b; c
(sequence (ident a) (sequence (ident b) (ident c)))
;; expr: while c do f () done
(while (ident c) (apply (ident f) [[nil (construct :unit)]]))
;; expr: for i = 1 to 10 do f i done
(for (var i) (constant (int "1")) (constant (int "10")) :to (apply (ident f) [[nil (ident i)]]))
;; expr: for i = 10 downto 1 do f i done
(for
 (var i)
 (constant (int "10"))
 (constant (int "1"))
 :downto
 (apply (ident f) [[nil (ident i)]]))
;; expr: for _ = 1 to 2 do () done
(for (any) (constant (int "1")) (constant (int "2")) :to (construct :unit))
;; expr: match x with A | B -> 1 | C y when y > 0 -> y | _ -> 0
(match
 (ident x)
 [(case (or (construct A) (construct B)) nil (constant (int "1")))
  (case
   (construct C [] (var y))
   (apply (ident (name ">")) [[nil (ident y)] [nil (constant (int "0"))]])
   (ident y))
  (case (any) nil (constant (int "0")))])
;; expr: try f () with Not_found | Failure _ -> 0
(try
 (apply (ident f) [[nil (construct :unit)]])
 [(case (or (construct Not_found) (construct Failure [] (any))) nil (constant (int "0")))])
;; expr: assert (x > 0)
(assert (apply (ident (name ">")) [[nil (ident x)] [nil (constant (int "0"))]]))
;; expr: assert false
(assert (construct :false))
;; expr: lazy (f ())
(lazy (apply (ident f) [[nil (construct :unit)]]))
;; expr: (e : int)
(constraint (ident e) (constr int []))
;; expr: (e :> t)
(coerce (ident e) nil (constr t []))
;; expr: (e : t0 :> t)
(coerce (ident e) (constr t0 []) (constr t []))
```

### 5.5 Bindings

A `binding` is `(binding P E [CONSTRAINT])` with its attributes and positions; the optional constraint of a
value binding is `(constraint [NAME...] T)` for `let x : type a. t = e` (`NAME`s the locally abstract types) or
`(coercion FROM TO)` for `let x :> t = e` and `let x : g :> t = e`. The parser keeps a `let f x = e` as the
function in `E`, and a constrained `let x : t = e` as the plain pattern `(var x)` with the value constraint
`(constraint [] T)` (S10); `let f (type a) (x : a) : a = x` puts the `: a` in the function's own `CONSTRAINT` (§5.1).

```forms
;; expr: let x = 1 in x
(let :nonrec [(binding (var x) (constant (int "1")))] (ident x))
;; expr: let rec f x = f x and g y = y in f
(let
 :rec
 [(binding
   (var f)
   (function [(param nil nil (var x))] nil (body (apply (ident f) [[nil (ident x)]]))))
  (binding (var g) (function [(param nil nil (var y))] nil (body (ident y))))]
 (ident f))
;; expr: let x : int = 1 in x
(let :nonrec [(binding (var x) (constant (int "1")) (constraint [] (constr int [])))] (ident x))
;; expr: let f : 'a. 'a -> 'a = fun x -> x in f
(let
 :nonrec
 [(binding
   (var f)
   (function [(param nil nil (var x))] nil (body (ident x)))
   (constraint [] (poly [a] (arrow nil (var a) (var a)))))]
 (ident f))
;; expr: let x :> t = e in x
(let :nonrec [(binding (var x) (ident e) (coercion nil (constr t [])))] (ident x))
;; expr: let (a, b) = p in a
(let :nonrec [(binding (tuple [(var a) (var b)] :closed) (ident p))] (ident a))
;; expr: let () = f () in ()
(let
 :nonrec
 [(binding (construct :unit) (apply (ident f) [[nil (construct :unit)]]))]
 (construct :unit))
;; expr: let f (type a) (x : a) : a = x in f
(let
 :nonrec
 [(binding
   (var f)
   (function
    [(newtype a) (param nil nil (constraint (var x) (constr a [])))]
    (constraint (constr a []))
    (body (ident x))))]
 (ident f))
;; expr: let x = 1 [@a] in x
(let :nonrec [(binding (var x) (constant (int "1") {:attrs [(attr a (str []))]}))] (ident x))
```

### 5.6 Local modules and opens

```forms
;; expr: let open M in e
(struct-item (open (ident M) :fresh) (ident e))
;; expr: M.(e)
(struct-item (open (ident M) :fresh) (ident e))
;; expr: let module L = List in L.length
(struct-item (module L (ident List)) (ident L.length))
;; expr: let exception E in raise E
(struct-item
 (exception (econstr E (decl [] [])))
 (apply (ident raise) [[nil (construct E)]]))
;; expr: (module M)
(pack (ident M))
;; expr: (module M : S)
(pack (ident M) (pkg S []))
;; expr: (module struct let x = 1 end : S)
(pack (structure [(value :nonrec [(binding (var x) (constant (int "1")))])]) (pkg S []))
```

### 5.7 Binding operators and extensions

```forms
;; expr: let* x = a in b
(letop (bop (name "let*") (var x) (ident a)) [] (ident b))
;; expr: let+ x = a and+ y = b in x + y
(letop
 (bop (name "let+") (var x) (ident a))
 [(bop (name "and+") (var y) (ident b))]
 (apply (ident (name "+")) [[nil (ident x)] [nil (ident y)]]))
;; expr: [%ext 1 + 2]
(extension
 (ext
  ext
  (str
   [(eval
     (apply (ident (name "+")) [[nil (constant (int "1"))] [nil (constant (int "2"))]]))])))
;; expr: [%ext]
(extension (ext ext (str [])))
;; expr: function A -> .
(function [] nil (cases [(case (construct A) nil (unreachable))]))
```

## 6. Classes and objects

Class expressions, class types and class fields. A class declaration is `(class-infos VIRTUAL [PARAM...] NAME BODY)`
(`'a class_infos`, with the attributes and positions), the body a class expression (`class`) or a class type
(`class type` and `class` in a signature).

| head | OCaml | fields |
|---|---|---|
| `(constr LID [T...])` | `c`, `['a] c` | class expression or class type |
| `(structure CSTRUCT)` | `object ... end` | class expression |
| `(signature CSIG)` | `object ... end` | class type |
| `(fun LABEL DEFAULT P CE)` | `fun p -> ce`, `fun ~l:p`, `fun ?l:(p = e)` | one parameter each |
| `(apply CE [[LABEL E]...])` | `ce a ~l:b` | |
| `(let FLAG [BINDING...] CE)` | `let x = e in ce` | |
| `(constraint CE CT)` | `(ce : ct)` | |
| `(arrow LABEL T CT)` | `t -> ct` | class type |
| `(open OPEN CE)` or `(open OPEN CT)` | `let open M in ce` | `OPEN` is an `(open LID FLAG)` |
| `(extension EXT)` | `[%id]` | |
| `(cstruct SELF [FIELD...])` | `object (self) ... end` | `SELF` a pattern (`(any)` when absent) |
| `(csig SELF [FIELD...])` | | `SELF` a type |

Class fields (`class_field`): `(inherit FLAG CE [NAME])` (`inherit`, `inherit!`, `as super`),
`(val NAME MUTABLE KIND)`, `(method NAME PRIVATE KIND)` with `KIND` = `(virtual T)` or `(concrete FLAG E)`
(`FLAG` `:fresh`, or `:override` for `method!` and `val!`; a method's `E` is a `(poly E [T])`),
`(constraint T1 T2)`, `(initializer E)`, `(attribute ATTR)`, `(extension EXT)`. Class type fields: `(inherit CT)`,
`(val NAME MUTABLE VIRTUAL T)`, `(method NAME PRIVATE VIRTUAL T)`, `(constraint T1 T2)`, `(attribute ATTR)`,
`(extension EXT)`.

```forms
;; item: class c = object end
(class [(class-infos :concrete [] c (structure (cstruct (any) [])))])
;; item: class point x = object (self) val mutable v = x method get = v method private p = 1 end
(class
 [(class-infos
   :concrete
   []
   point
   (fun
    nil
    nil
    (var x)
    (structure
     (cstruct
      (var self)
      [(val v :mutable (concrete :fresh (ident x)))
       (method get :public (concrete :fresh (poly (ident v))))
       (method p :private (concrete :fresh (poly (constant (int "1")))))]))))])
;; item: class virtual ['a] v = object method virtual m : 'a end
(class
 [(class-infos
   :virtual
   [[(var a) nil nil]]
   v
   (structure (cstruct (any) [(method m :public (virtual (var a)))])))])
;; item: class sub x = object inherit point x as super method! get = super#get + 1 end
(class
 [(class-infos
   :concrete
   []
   sub
   (fun
    nil
    nil
    (var x)
    (structure
     (cstruct
      (any)
      [(inherit :fresh (apply (constr point []) [[nil (ident x)]]) super)
       (method
        get
        :public
        (concrete
         :override
         (poly
          (apply
           (ident (name "+"))
           [[nil (send (ident super) get)] [nil (constant (int "1"))]]))))]))))])
;; item: class type ct = object method m : int end
(class-type
 [(class-infos
   :concrete
   []
   ct
   (signature (csig (any) [(method m :public :concrete (constr int []))])))])
;; item: class c2 = [int] v
(class [(class-infos :concrete [] c2 (constr v [(constr int [])]))])
;; item: class c3 = fun ~l ?(o = 1) (p : int) -> object end
(class
 [(class-infos
   :concrete
   []
   c3
   (fun
    :l
    nil
    (var l)
    (fun
     :?o
     (constant (int "1"))
     (var o)
     (fun nil nil (constraint (var p) (constr int [])) (structure (cstruct (any) []))))))])
;; item: class c4 = let z = 1 in object val q = z end
(class
 [(class-infos
   :concrete
   []
   c4
   (let
    :nonrec
    [(binding (var z) (constant (int "1")))]
    (structure (cstruct (any) [(val q :immutable (concrete :fresh (ident z)))]))))])
;; item: class c5 : ct = object method m = 1 end
(class
 [(class-infos
   :concrete
   []
   c5
   (constraint
    (structure
     (cstruct (any) [(method m :public (concrete :fresh (poly (constant (int "1")))))]))
    (constr ct [])))])
;; item: class c = object constraint 'a = int initializer f () end
(class
 [(class-infos
   :concrete
   []
   c
   (structure
    (cstruct
     (any)
     [(constraint (var a) (constr int []))
      (initializer (apply (ident f) [[nil (construct :unit)]]))])))])
;; item: class c = object method m : 'a. 'a -> 'a = fun a -> a end
(class
 [(class-infos
   :concrete
   []
   c
   (structure
    (cstruct
     (any)
     [(method
       m
       :public
       (concrete
        :fresh
        (poly
         (function [(param nil nil (var a))] nil (body (ident a)))
         (poly [a] (arrow nil (var a) (var a))))))])))])
;; item: class c = object val virtual z : int end
(class
 [(class-infos
   :concrete
   []
   c
   (structure (cstruct (any) [(val z :immutable (virtual (constr int [])))])))])
;; expr: object method m = 1 end
(object (cstruct (any) [(method m :public (concrete :fresh (poly (constant (int "1")))))]))
;; expr: new point 1
(apply (new point) [[nil (constant (int "1"))]])
;; expr: o#get
(send (ident o) get)
;; expr: {< x = 2 >}
(override [[x (constant (int "2"))]])
```

## 7. The module language

### 7.1 Module types

| head | OCaml | fields |
|---|---|---|
| `(ident LID)` | `S`, `M.S` | |
| `(signature [ITEM...])` | `sig ... end` | §8.3 |
| `(functor PARAM MT)` | `functor (X : S) -> MT`, `(X : S) -> MT` | `PARAM` is `:unit` for `()` or `(named NAME MT)` (`NAME` `nil` for `_`) |
| `(with MT [CONSTRAINT...])` | `MT with type t = T and module M = N` | §7.3 |
| `(typeof ME)` | `module type of ME` | |
| `(extension EXT)` | `[%id]` | |
| `(alias LID)` | `module N = M` in a signature | the module type of a signature item `module N = M` (`Psig_module` with `Pmty_alias`; found at O2, stdlib's `stdLabels.mli`: the O1 text said the parser never produces it); the printer writes the item, not `(module M)` |

### 7.2 Module expressions

| head | OCaml | fields |
|---|---|---|
| `(ident LID)` | `M`, `M.N` | |
| `(structure [ITEM...])` | `struct ... end` | §8.3 |
| `(functor PARAM ME)` | `functor (X : S) -> ME` | |
| `(apply ME1 ME2)` | `F(X)` | |
| `(apply-unit ME)` | `F()` | OCaml 5.2's generative application |
| `(constraint ME MT)` | `(ME : MT)` | |
| `(unpack E)` | `(val e)` | |
| `(extension EXT)` | `[%id]` | |

### 7.3 `with` constraints

`(type LID DECL)` (`with type t = T`, the `decl` carrying the name, parameters, manifest, private flag),
`(module LID LID)` (`with module M = N`), `(modtype LID MT)` (`with module type T = MT`),
`(typesubst LID DECL)` (`with type t := T`), `(modsubst LID LID)` (`with module M := N`),
`(modtypesubst LID MT)` (`with module type T := MT`).

```forms
;; modtype: S
(ident S)
;; modtype: sig type t val x : t end
(signature [(type :rec [(decl t [] [] :abstract :public)]) (value x (constr t []) [])])
;; modtype: functor (X : S) -> S
(functor (named X (ident S)) (ident S))
;; modtype: functor () -> S
(functor :unit (ident S))
;; modtype: functor (_ : S) -> S
(functor (named nil (ident S)) (ident S))
;; modtype: S with type t = int and module M = N
(with (ident S) [(type t (decl t [] [] :abstract :public (constr int []))) (module M N)])
;; modtype: S with type 'a t := 'a list
(with
 (ident S)
 [(typesubst t (decl t [[(var a) nil nil]] [] :abstract :public (constr list [(var a)])))])
;; modtype: S with module type T = U
(with (ident S) [(modtype T (ident U))])
;; modtype: S with module M := N
(with (ident S) [(modsubst M N)])
;; modtype: module type of M
(typeof (ident M))
;; modtype: [%mt]
(extension (ext mt (str [])))
;; module: M.N
(ident M.N)
;; module: struct type t = int end
(structure [(type :rec [(decl t [] [] :abstract :public (constr int []))])])
;; module: functor (X : S) -> X
(functor (named X (ident S)) (ident X))
;; module: F (X) (Y)
(apply (apply (ident F) (ident X)) (ident Y))
;; module: F ()
(apply-unit (ident F))
;; module: (M : S)
(constraint (ident M) (ident S))
;; module: (val m)
(unpack (ident m))
;; module: [%me]
(extension (ext me (str [])))
```

## 8. Items, attributes and extensions

### 8.1 Attributes

An attribute `[@name payload]` is `(attr NAME PAYLOAD)` (with `:loc`), listed in the node's `:attrs`; a floating
`[@@@name payload]` is the item `(attribute (attr ...))`. A payload is `(str [ITEM...])` (`[@a e]`, `[@a let x = 1]`),
`(sig [ITEM...])` (`[@a: val x : t]`), `(typ T)` (`[@a: t]`) or `(pat P [GUARD])` (`[@a? p when e]`). A
docstring `(** ... *)` is the attribute `ocaml.doc`, a floating one `ocaml.text`. An attribute's name keeps its dots and is
written like a long identifier (§2.2): `(attr ocaml.doc (str [...]))`.

### 8.2 Extension points

`[%name payload]` is `(ext NAME PAYLOAD)` inside an `(extension EXT)` of the category (a type, pattern, expression,
class, module or item). `let%ext x = e` is the parser's desugaring: the item `(extension (ext ext (str [(value ...)])))`,
the same tree as `[%%ext let x = e]`; an item `[%%name ...]` is `(extension EXT)` with its attributes in the map.

### 8.3 Items

| head | OCaml | fields | |
|---|---|---|---|
| `(eval E)` | `e` (at toplevel) | | `Pstr_eval`, attributes in the map |
| `(value FLAG [BINDING...])` | `let ... and ...` | | `Pstr_value` |
| `(primitive NAME T [PRIM...])` | `external x : t = "s1" "s2"` | | `Pstr_primitive` |
| `(value NAME T [PRIM...])` | `val x : t`, `external x : t = "p"` | | `Psig_value` |
| `(type FLAG [DECL...])` | `type t = ... and ...`, `type nonrec t` | | `Pstr_type`, `Psig_type` |
| `(typesubst [DECL...])` | `type t := T` | | `Psig_typesubst` |
| `(typext LID [PARAM...] [ECONSTR...] PRIVATE)` | `type t += A \| B of int` | | `Pstr_typext`, `Psig_typext` |
| `(exception ECONSTR)` | `exception E of int`, `exception E = M.E` | | `Pstr_exception`, `Psig_exception` |
| `(module NAME ME)` | `module M = ME`, `module M : MT = ME` | | `Pstr_module`; `NAME` is `nil` for `_` |
| `(module NAME MT)` | `module M : MT` | | `Psig_module` |
| `(recmodule [(modbind NAME ME)...])` | `module rec A = ... and B = ...` | | `Pstr_recmodule` |
| `(recmodule [(moddecl NAME MT)...])` | `module rec A : S and B : T` | | `Psig_recmodule` |
| `(modsubst NAME LID)` | `module M := N` | | `Psig_modsubst` |
| `(modtype NAME [MT])` | `module type S = MT`, `module type S` | | `Pstr_modtype`, `Psig_modtype` |
| `(modtypesubst NAME MT)` | `module type S := MT` | | `Psig_modtypesubst` |
| `(open ME FLAG)` | `open M`, `open struct ... end`, `open! M` | | `Pstr_open` |
| `(open LID FLAG)` | `open M` | | `Psig_open` |
| `(include ME)` | `include M` | | `Pstr_include` |
| `(include MT)` | `include S` | | `Psig_include` |
| `(class [CLASS-INFOS...])` | `class c = ... and d = ...` | | `Pstr_class`, `Psig_class` |
| `(class-type [CLASS-INFOS...])` | `class type c = ...` | | `Pstr_class_type`, `Psig_class_type` |
| `(attribute ATTR)` | `[@@@a]` | | `Pstr_attribute`, `Psig_attribute` |
| `(extension EXT)` | `[%%e]` | | `Pstr_extension`, `Psig_extension` |

(`value`, `module`, `open`, `include` are the heads of both the structure and the signature item: the slot
decides, §1.2; the structure `value` has a flag and bindings, the signature `value` a name and a type.)

A type declaration is `(decl NAME [PARAM...] [CONSTRAINT...] KIND PRIVATE [MANIFEST])`, `KIND`:

| KIND | OCaml |
|---|---|
| `:abstract` | `type t`, `type t = T` (with a manifest) |
| `(variant [CDECL...])` | `type t = A \| B of int` |
| `(record [LDECL...])` | `type t = { l : T; mutable m : U }` |
| `:open` | `type t = ..` |
| `(external "gmp")` | `type t = external "gmp"` (`Ptype_external`) |

`CONSTRAINT` is `(constraint T1 T2)` (`constraint 'a = int`); `LDECL` is `(ldecl NAME MUTABLE T)`; `CDECL` is
`(cdecl NAME [VAR...] ARGS [RES])`, where `ARGS` is a vector of types (`C of T1 * T2`) or `(record [LDECL...])`
(`C of { ... }`), `VAR`s the existential variables of a GADT constructor (`C : 'a. ...`) and `RES` its result
type. An extension constructor is `(econstr NAME KIND)` with `KIND` = `(decl [VAR...] ARGS [RES])` or
`(rebind LID)`.

```forms
;; item: let x = 1
(value :nonrec [(binding (var x) (constant (int "1")))])
;; item: let rec f x = g x and g y = f y
(value
 :rec
 [(binding
   (var f)
   (function [(param nil nil (var x))] nil (body (apply (ident g) [[nil (ident x)]]))))
  (binding
   (var g)
   (function [(param nil nil (var y))] nil (body (apply (ident f) [[nil (ident y)]]))))])
;; item: let () = ()
(value :nonrec [(binding (construct :unit) (construct :unit))])
;; item: 1 + 2
(eval (apply (ident (name "+")) [[nil (constant (int "1"))] [nil (constant (int "2"))]]))
;; item: type t = int
(type :rec [(decl t [] [] :abstract :public (constr int []))])
;; item: type 'a l = Nil | Cons of 'a * 'a l
(type
 :rec
 [(decl
   l
   [[(var a) nil nil]]
   []
   (variant [(cdecl Nil [] []) (cdecl Cons [] [(var a) (constr l [(var a)])])])
   :public)])
;; item: type r = { a : int; mutable b : string }
(type
 :rec
 [(decl
   r
   []
   []
   (record [(ldecl a :immutable (constr int [])) (ldecl b :mutable (constr string []))])
   :public)])
;; item: type +'a cov = 'a list
(type :rec [(decl cov [[(var a) :+ nil]] [] :abstract :public (constr list [(var a)]))])
;; item: type !'a inj = 'a ref
(type :rec [(decl inj [[(var a) nil :!]] [] :abstract :public (constr ref [(var a)]))])
;; item: type ('a, 'b) pair = 'a * 'b constraint 'a = int
(type
 :rec
 [(decl
   pair
   [[(var a) nil nil] [(var b) nil nil]]
   [(constraint (var a) (constr int []))]
   :abstract
   :public
   (tuple [(var a) (var b)]))])
;; item: type priv = private A | B
(type :rec [(decl priv [] [] (variant [(cdecl A [] []) (cdecl B [] [])]) :private)])
;; item: type pm = private int
(type :rec [(decl pm [] [] :abstract :private (constr int []))])
;; item: type e = ..
(type :rec [(decl e [] [] :open :public)])
;; item: type nonrec t2 = t
(type :nonrec [(decl t2 [] [] :abstract :public (constr t []))])
;; item: type t = int and u = string
(type
 :rec
 [(decl t [] [] :abstract :public (constr int []))
  (decl u [] [] :abstract :public (constr string []))])
;; item: type _ g = I : int -> int g | P : 'a g * 'b g -> ('a * 'b) g
(type
 :rec
 [(decl
   g
   [[(any) nil nil]]
   []
   (variant
    [(cdecl I [] [(constr int [])] (constr g [(constr int [])]))
     (cdecl P [] [(constr g [(var a)]) (constr g [(var b)])] (constr g [(tuple [(var a) (var b)])]))])
   :public)])
;; item: type 'a ex = Ex : 'b * ('b -> 'a) -> 'a ex
(type
 :rec
 [(decl
   ex
   [[(var a) nil nil]]
   []
   (variant [(cdecl Ex [] [(var b) (arrow nil (var b) (var a))] (constr ex [(var a)]))])
   :public)])
;; item: type inl = C of { x : int; y : int } | D
(type
 :rec
 [(decl
   inl
   []
   []
   (variant
    [(cdecl
      C
      []
      (record [(ldecl x :immutable (constr int [])) (ldecl y :immutable (constr int []))]))
     (cdecl D [] [])])
   :public)])
;; item: type g2 = G : { x : int } -> g2
(type
 :rec
 [(decl
   g2
   []
   []
   (variant [(cdecl G [] (record [(ldecl x :immutable (constr int []))]) (constr g2 []))])
   :public)])
;; item: type t = external "gmp"
(type :rec [(decl t [] [] (external "gmp") :public)])
;; item: type e += E1 of int | E2
(typext e [] [(econstr E1 (decl [] [(constr int [])])) (econstr E2 (decl [] []))] :public)
;; item: type e += private E3
(typext e [] [(econstr E3 (decl [] []))] :private)
;; item: exception Exn of int
(exception (econstr Exn (decl [] [(constr int [])])))
;; item: exception Exn2 = Exn
(exception (econstr Exn2 (rebind Exn)))
;; item: external id : 'a -> 'a = "%identity"
(primitive id (arrow nil (var a) (var a)) ["%identity"])
;; item: module M = struct end
(module M (structure []))
;; item: module M : S = struct end
(module M (constraint (structure []) (ident S)))
;; item: module _ = M
(module nil (ident M))
;; item: module F (X : S) (Y : sig end) = struct include X end
(module
 F
 (functor
  (named X (ident S))
  (functor (named Y (signature [])) (structure [(include (ident X))]))))
;; item: module G (_ : S) = struct end
(module G (functor (named nil (ident S)) (structure [])))
;; item: module H () = struct end
(module H (functor :unit (structure [])))
;; item: module type S = sig end
(modtype S (signature []))
;; item: module type Abs
(modtype Abs)
;; item: module rec A : S = struct end and B : S = struct end
(recmodule
 [(modbind A (constraint (structure []) (ident S)))
  (modbind B (constraint (structure []) (ident S)))])
;; item: open M
(open (ident M) :fresh)
;; item: open! M
(open (ident M) :override)
;; item: open struct let hidden = 1 end
(open (structure [(value :nonrec [(binding (var hidden) (constant (int "1")))])]) :fresh)
;; item: include M
(include (ident M))
;; item: include (M : S)
(include (constraint (ident M) (ident S)))
;; item: let x = 1 [@@a]
(value :nonrec [(binding (var x) (constant (int "1")) {:attrs [(attr a (str []))]})])
;; item: [@@@warning "-a"]
(attribute (attr warning (str [(eval (constant (string "-a")))])))
;; item: [%%ext let x = 1]
(extension (ext ext (str [(value :nonrec [(binding (var x) (constant (int "1")))])])))
;; item: let%ext x = 1
(extension (ext ext (str [(value :nonrec [(binding (var x) (constant (int "1")))])])))
```

(`include functor`, which the survey's O1 row names, is not accepted by the 5.5.0 parser: `include functor F` is a
syntax error (Appendix A, E7), so there is no form for it; the survey's row listed it from memory, S25.)

```forms
;; sigitem: val x : int
(value x (constr int []) [])
;; sigitem: val poly : 'a 'b. 'a -> 'b -> 'a
(value poly (poly [a b] (arrow nil (var a) (arrow nil (var b) (var a)))) [])
;; sigitem: val f : ?opt:int -> lbl:string -> 'a list -> 'a option
(value
 f
 (arrow
  :?opt
  (constr int [])
  (arrow :lbl (constr string []) (arrow nil (constr list [(var a)]) (constr option [(var a)]))))
 [])
;; sigitem: external g : int -> int = "caml_g" "caml_g_unboxed" [@@noalloc]
(value
 g
 (arrow nil (constr int []) (constr int []))
 ["caml_g" "caml_g_unboxed"]
 {:attrs [(attr noalloc (str []))]})
;; sigitem: type t
(type :rec [(decl t [] [] :abstract :public)])
;; sigitem: type u := t
(typesubst [(decl u [] [] :abstract :public (constr t []))])
;; sigitem: type e += X
(typext e [] [(econstr X (decl [] []))] :public)
;; sigitem: exception E of string
(exception (econstr E (decl [] [(constr string [])])))
;; sigitem: module M : sig type u end
(module M (signature [(type :rec [(decl u [] [] :abstract :public)])]))
;; sigitem: module N := M
(modsubst N M)
;; sigitem: module type S = sig end
(modtype S (signature []))
;; sigitem: module type S := sig end
(modtypesubst S (signature []))
;; sigitem: module rec A : sig end and B : sig end
(recmodule [(moddecl A (signature [])) (moddecl B (signature []))])
;; sigitem: open List
(open List :fresh)
;; sigitem: include S
(include (ident S))
;; sigitem: include S with type t = int
(include (with (ident S) [(type t (decl t [] [] :abstract :public (constr int [])))]))
;; sigitem: class c : object method m : int end
(class
 [(class-infos
   :concrete
   []
   c
   (signature (csig (any) [(method m :public :concrete (constr int []))])))])
;; sigitem: class type ct = object end
(class-type [(class-infos :concrete [] ct (signature (csig (any) [])))])
;; sigitem: [@@@warning "-a"]
(attribute (attr warning (str [(eval (constant (string "-a")))])))
;; sigitem: [%%ext: int]
(extension (ext ext (typ (constr int []))))
```

### 8.4 Whole files

```forms
;; impl: let x = 1 + 2
(ocaml/impl
 [(value
   :nonrec
   [(binding
     (var x)
     (apply (ident (name "+")) [[nil (constant (int "1"))] [nil (constant (int "2"))]]))])])
;; intf: val x : int
(ocaml/intf [(value x (constr int []) [])])
```

### 8.5 The top-level forms

A file is `(ocaml/impl [ITEM...] {:file "x.ml" :ocaml "5.5.0"})` for an `.ml` (a `structure`) and
`(ocaml/intf [ITEM...] {...})` for an `.mli` (a `signature`). `:file` is the name the reader was given (`--file`).
The head is qualified (`ocaml/impl`) so that a file of forms says what it is (S11); the heads inside are not
qualified: the forms are data, not code.

## 9. Toplevel phrases and errors

`Parse.use_file` (`ocdump --use-file`) gives `(ocaml/toplevel [PHRASE...] {...})`, each phrase `(def [ITEM...])`
(`Ptop_def`) or `(directive NAME [ARG])` (`Ptop_dir`: `#use "f.ml"`, `#load "x"`, `#directory "d"`, `#show M`), the
argument `(string "f")`, `(int "1")`, `(ident LID)` or `(bool true)` (`Pdir_string`, `Pdir_int`, `Pdir_ident`,
`Pdir_bool`).

A file the compiler's parser rejects is `(ocaml/error KIND MESSAGE {:loc ...})` instead of the file form, with
`KIND` `:syntax` (`Syntaxerr.Error`), `:lexer` (`Lexer.Error`), `:other` (any other located error) or
`:exception` (an exception without a location). `MESSAGE` is the compiler's text without the location. The
helper exits 0: an error is a result, judged by the check on message and location, as goi's errorcheck tests
are (survey §5.4).

## 10. Positions

With `--loc` (positions on, S15) every node carries `:loc` in its metadata map:
`[start-line start-col end-line end-col start-offset end-offset]`. Lines count from 1; columns are *bytes* from the
line start; offsets are bytes from the file start (the `Lexing.position`'s `pos_cnum`). A **ghost** location
(`loc_ghost`) adds `:ghost true`: the parser invented the node (the second `fun` of `fun x y ->`, the `::` of a
list literal, the unparenthesised tuple). A name or long identifier in a slot that the Parsetree gives a
`loc` (`string loc`, `Longident.t loc`) is `(at NAME LOC)` with `LOC` the same six numbers. Removing every `:loc`,
`:ghost`, `:stack`, `:content-loc` and unwrapping every `at` gives the forms without positions, mode `:lines`
(§1.1): the two are one function of each other, which the converter of O2 relies on (S16).

```forms
;; expr: f x
(apply (ident f) [[nil (ident x)]])
```

(The block above is a forms-only example, checked to read, not against the helper.) With positions on, the same
expression of `let _ = f x` read at the file's second column is, e.g.,
`(apply (ident (at f [1 8 1 9 8 9]) {:loc [1 8 1 9 8 9]}) [[nil (ident (at x [1 10 1 11 10 11]) {:loc [1 10 1 11 10 11]})]] {:loc [1 8 1 11 8 11]})`.

## 11. The helper's other modes

`ocdump --mode M`:

- **`parse`** (the default): §3-§9.
- **`types`** (OC4 (a)): the compiler's typing of the file (`Compile_common.typecheck_impl`, warnings off, no output
  files, the standard library's `.cmi` from the host) as
  `(ocaml/typed SIGNATURE [ANNOTATION...] {...})`: the inferred signature as the compiler prints it, then one
  `(expr "type" {:loc ...})` per expression and `(pat "type" {:loc ...})` per pattern of the Typedtree (ghost nodes
  left out), the type printed in the node's environment. This is **annotations**, not the Typedtree as a tree
  (S19): what the toplevel's value printing and the error judging need (survey §6.3); a Typedtree as forms is a
  proposal (OC20 (b)). A typing error is `(ocaml/error :other MESSAGE ...)`.
- **`lambda`** (OC4 (a), OC5 (a)): the file typed and translated by `Translmod.transl_implementation` (the bytecode
  translation: no flambda on the host, and the closure backend's store translation needs a native context), as
  `(ocaml/lambda (lambda-program MODULE SIZE [REQUIRED...] CODE) {...})`. Identifiers are their unique names
  (`x_274`); the nodes are `(var id)`, `(mutvar id)`, `(const C)`, `(apply F [ARG...])`,
  `(function KIND [[id vk]...] RETURN BODY)` (`:curried` or `:tupled`; value kinds as the compiler prints them),
  `(let LETKIND VK id DEF BODY)` (`:strict` `:alias` `:strictopt`), `(mutlet ...)`, `(letrec [[id FN]...] BODY)`,
  `(prim "name" [ARG...])` (the primitive as `Printlambda.primitive` prints it: `"makeblock 0"`, `"+"`, `"setglobal M!"`),
  `(switch E numconsts consts numblocks blocks fail)`, `(stringswitch E [[s E]...] DEFAULT)`,
  `(staticraise n [ARG...])`, `(staticcatch BODY [n [[id vk]...]] HANDLER)`, `(trywith BODY id HANDLER)`,
  `(ifthenelse C T F)`, `(sequence A B)`, `(while C B)`, `(for id LO HI DIR BODY)`, `(assign id E)`,
  `(send KIND M O [ARG...])`, `(event E)`, `(ifused id E)`; constants `(int n)`, `(char n)`, `(float "t")`, `(int32 "n")`,
  `(int64 "n")`, `(nativeint "n")`, `(block tag [C...])`, `(float-array ["t"...])`, `(immstring "s")`. Positions
  (always on for `types`; with `--loc` for `lambda`) are `:loc` metadata. Effects are ordinary calls into
  the `Stdlib__Effect` global's fields in the sample's Lambda: `Effect.perform` is a library function over the
  runtime's primitives, so O5 meets them as `external` calls.
- **`tokens`** (O3's input): the lexer's token stream with the comments, `(ocaml/tokens [(tok "text" LOC)...])`, `LOC`
  `nil` without `--loc`. Text is the lexeme as written.

## 12. The checklist

Every constructor of `parsing/parsetree.mli` and `parsing/asttypes.mli` of the pinned 5.5.0, with its form. A
`?` marks a trailing option (§1.2); `[...]` is a vector. `bin/repr-ocaml spec-check` fails when a constructor of
those files is missing from this section.

### 12.1 Constants (`Pconst_*`, §2.5)

`Pconst_integer` is `(int "t" [SFX])`, `Pconst_char` `(char N)`, `Pconst_string` `(string S [DELIM])`,
`Pconst_float` `(float "t" [SFX])`. (`Asttypes.constant`'s `Const_int`, `Const_char`, `Const_string`, `Const_float`,
`Const_int32`, `Const_int64`, `Const_nativeint` are the Typedtree's: the typed annotations print types, not
constants, and Lambda's constants are §11's `(int n)`... forms.)

### 12.2 Types (`Ptyp_*`, `Rtag`, `Rinherit`, `Otag`, `Oinherit`, §3)

`Ptyp_any` `(any)` · `Ptyp_var` `(var N)` · `Ptyp_arrow` `(arrow L T T)` · `Ptyp_tuple` `(tuple [E...])` ·
`Ptyp_constr` `(constr LID [T...])` · `Ptyp_object` `(object [F...] FLAG)` · `Ptyp_class` `(class LID [T...])` ·
`Ptyp_alias` `(alias T N)` · `Ptyp_variant` `(variant [R...] FLAG LABELS)` · `Ptyp_poly` `(poly [N...] T)` ·
`Ptyp_package` `(package PKG)` · `Ptyp_open` `(open LID T)` · `Ptyp_extension` `(extension EXT)` ·
`Ptyp_functor` `(functor L N PKG T)` · `Rtag` `(rtag N BOOL [T...])` · `Rinherit` `(rinherit T)` ·
`Otag` `(tag N T)` · `Oinherit` `(inherit T)` · `package_type` `(pkg LID [[LID T]...])`.

### 12.3 Patterns (`Ppat_*`, §4)

`Ppat_any` `(any)` · `Ppat_var` `(var N)` · `Ppat_alias` `(alias P N)` · `Ppat_constant` `(constant C)` ·
`Ppat_interval` `(interval C C)` · `Ppat_tuple` `(tuple [E...] FLAG)` · `Ppat_construct` `(construct LID [VARS P]?)` ·
`Ppat_variant` `(variant N P?)` · `Ppat_record` `(record [[LID P]...] FLAG)` · `Ppat_array` `(array [P...])` ·
`Ppat_or` `(or P P)` · `Ppat_constraint` `(constraint P T)` · `Ppat_type` `(type LID)` · `Ppat_lazy` `(lazy P)` ·
`Ppat_unpack` `(unpack N PKG?)` · `Ppat_exception` `(exception P)` · `Ppat_effect` `(effect P P)` ·
`Ppat_extension` `(extension EXT)` · `Ppat_open` `(open LID P)`.

### 12.4 Expressions (`Pexp_*`, `Pparam_*`, `Pfunction_*`, `Pconstraint`, `Pcoerce`, §5)

`Pexp_ident` `(ident LID)` · `Pexp_constant` `(constant C)` · `Pexp_let` `(let FLAG [B...] E)` ·
`Pexp_function` `(function [P...] C BODY)` · `Pexp_apply` `(apply E [[L E]...])` · `Pexp_match` `(match E [C...])` ·
`Pexp_try` `(try E [C...])` · `Pexp_tuple` `(tuple [E...])` · `Pexp_construct` `(construct LID E?)` ·
`Pexp_variant` `(variant N E?)` · `Pexp_record` `(record [[LID E]...] E?)` · `Pexp_field` `(field E LID)` ·
`Pexp_setfield` `(setfield E LID E)` · `Pexp_array` `(array [E...])` · `Pexp_ifthenelse` `(ifthenelse E E E?)` ·
`Pexp_sequence` `(sequence E E)` · `Pexp_while` `(while E E)` · `Pexp_for` `(for P E E DIR E)` ·
`Pexp_constraint` `(constraint E T)` · `Pexp_coerce` `(coerce E T? T)` (`from` is `nil`, not omitted) ·
`Pexp_send` `(send E N)` · `Pexp_new` `(new LID)` · `Pexp_setinstvar` `(setinstvar N E)` ·
`Pexp_override` `(override [[N E]...])` · `Pexp_struct_item` `(struct-item ITEM E)` · `Pexp_assert` `(assert E)` ·
`Pexp_lazy` `(lazy E)` · `Pexp_poly` `(poly E T?)` · `Pexp_object` `(object CSTRUCT)` · `Pexp_newtype` `(newtype N E)` ·
`Pexp_pack` `(pack ME PKG?)` · `Pexp_letop` `(letop BOP [BOP...] E)` · `Pexp_extension` `(extension EXT)` ·
`Pexp_unreachable` `(unreachable)` · `case` `(case P G E)` · `binding_op` `(bop N P E)` ·
`Pparam_val` `(param L D P)` · `Pparam_newtype` `(newtype N)` · `Pfunction_body` `(body E)` ·
`Pfunction_cases` `(cases [C...])` · `Pconstraint` `(constraint T)` · `Pcoerce` `(coerce T? T)`.

### 12.5 Declarations (`Ptype_*`, `Pcstr_*`, `Pext_*`, `Pvc_*`, §8.3)

`value_description` `(value N T [PRIM...])` / `(primitive N T [PRIM...])` · `type_declaration`
`(decl N [PARAM...] [CON...] KIND PRIV T?)` · `Ptype_abstract` `:abstract` · `Ptype_variant` `(variant [CDECL...])` ·
`Ptype_record` `(record [LDECL...])` · `Ptype_open` `:open` · `Ptype_external` `(external "s")` ·
`label_declaration` `(ldecl N MUT T)` · `constructor_declaration` `(cdecl N [V...] ARGS T?)` · `Pcstr_tuple` `[T...]` ·
`Pcstr_record` `(record [LDECL...])` · `type_extension` `(typext LID [PARAM...] [E...] PRIV)` ·
`extension_constructor` `(econstr N KIND)` · `Pext_decl` `(decl [V...] ARGS T?)` · `Pext_rebind` `(rebind LID)` ·
`type_exception` `(exception ECONSTR)` · `Pvc_constraint` `(constraint [N...] T)` · `Pvc_coercion` `(coercion T? T)` ·
`value_binding` `(binding P E VC?)`.

### 12.6 Classes (`Pcty_*`, `Pctf_*`, `Pcl_*`, `Pcf_*`, `Cfk_*`, §6)

`Pcty_constr` `(constr LID [T...])` · `Pcty_signature` `(signature CSIG)` · `Pcty_arrow` `(arrow L T CT)` ·
`Pcty_extension` `(extension EXT)` · `Pcty_open` `(open OPEN CT)` · `class_signature` `(csig T [F...])` ·
`Pctf_inherit` `(inherit CT)` · `Pctf_val` `(val N MUT VIRT T)` · `Pctf_method` `(method N PRIV VIRT T)` ·
`Pctf_constraint` `(constraint T T)` · `Pctf_attribute` `(attribute A)` · `Pctf_extension` `(extension EXT)` ·
`class_infos` `(class-infos VIRT [PARAM...] N BODY)` · `Pcl_constr` `(constr LID [T...])` ·
`Pcl_structure` `(structure CSTRUCT)` · `Pcl_fun` `(fun L D P CE)` · `Pcl_apply` `(apply CE [[L E]...])` ·
`Pcl_let` `(let FLAG [B...] CE)` · `Pcl_constraint` `(constraint CE CT)` · `Pcl_extension` `(extension EXT)` ·
`Pcl_open` `(open OPEN CE)` · `class_structure` `(cstruct P [F...])` · `Pcf_inherit` `(inherit FLAG CE N?)` ·
`Pcf_val` `(val N MUT KIND)` · `Pcf_method` `(method N PRIV KIND)` · `Pcf_constraint` `(constraint T T)` ·
`Pcf_initializer` `(initializer E)` · `Pcf_attribute` `(attribute A)` · `Pcf_extension` `(extension EXT)` ·
`Cfk_virtual` `(virtual T)` · `Cfk_concrete` `(concrete FLAG E)`.

### 12.7 Modules (`Pmty_*`, `Pmod_*`, `Pwith_*`, `Psig_*`, `Pstr_*`, `Named`, `Unit`, §7, §8.3)

`Pmty_ident` `(ident LID)` · `Pmty_signature` `(signature [I...])` · `Pmty_functor` `(functor P MT)` ·
`Pmty_with` `(with MT [W...])` · `Pmty_typeof` `(typeof ME)` · `Pmty_extension` `(extension EXT)` ·
`Pmty_alias` `(alias LID)` · `Unit` `:unit` · `Named` `(named N MT)` · `Pwith_type` `(type LID DECL)` ·
`Pwith_module` `(module LID LID)` · `Pwith_modtype` `(modtype LID MT)` · `Pwith_modtypesubst`
`(modtypesubst LID MT)` · `Pwith_typesubst` `(typesubst LID DECL)` · `Pwith_modsubst` `(modsubst LID LID)` ·
`Pmod_ident` `(ident LID)` · `Pmod_structure` `(structure [I...])` · `Pmod_functor` `(functor P ME)` ·
`Pmod_apply` `(apply ME ME)` · `Pmod_apply_unit` `(apply-unit ME)` · `Pmod_constraint` `(constraint ME MT)` ·
`Pmod_unpack` `(unpack E)` · `Pmod_extension` `(extension EXT)` · `Psig_value` `(value N T [P...])` ·
`Psig_type` `(type FLAG [D...])` · `Psig_typesubst` `(typesubst [D...])` · `Psig_typext` `(typext ...)` ·
`Psig_exception` `(exception EC)` · `Psig_module` `(module N MT)` · `Psig_modsubst` `(modsubst N LID)` ·
`Psig_recmodule` `(recmodule [(moddecl N MT)...])` · `Psig_modtype` `(modtype N MT?)` ·
`Psig_modtypesubst` `(modtypesubst N MT)` · `Psig_open` `(open LID FLAG)` · `Psig_include` `(include MT)` ·
`Psig_class` `(class [CI...])` · `Psig_class_type` `(class-type [CI...])` · `Psig_attribute` `(attribute A)` ·
`Psig_extension` `(extension EXT)` · `Pstr_eval` `(eval E)` · `Pstr_value` `(value FLAG [B...])` ·
`Pstr_primitive` `(primitive N T [P...])` · `Pstr_type` `(type FLAG [D...])` · `Pstr_typext` `(typext ...)` ·
`Pstr_exception` `(exception EC)` · `Pstr_module` `(module N ME)` · `Pstr_recmodule`
`(recmodule [(modbind N ME)...])` · `Pstr_modtype` `(modtype N MT?)` · `Pstr_open` `(open ME FLAG)` ·
`Pstr_class` `(class [CI...])` · `Pstr_class_type` `(class-type [CI...])` · `Pstr_include` `(include ME)` ·
`Pstr_attribute` `(attribute A)` · `Pstr_extension` `(extension EXT)` · `module_declaration`, `module_binding`,
`module_type_declaration`, `module_substitution`, `open_infos`, `include_infos` are carried by the item forms
above (their attributes and positions in the item's map).

### 12.8 Attributes, payloads and the toplevel (`PStr`, `PSig`, `PTyp`, `PPat`, `Ptop_*`, `Pdir_*`, §8.1, §9)

`attribute` `(attr N PAYLOAD)` · `extension` `(ext N PAYLOAD)` · `PStr` `(str [I...])` · `PSig` `(sig [I...])` ·
`PTyp` `(typ T)` · `PPat` `(pat P E?)` · `Ptop_def` `(def [I...])` · `Ptop_dir` `(directive N ARG?)` ·
`Pdir_string` `(string S)` · `Pdir_int` `(int "t" SFX?)` · `Pdir_ident` `(ident LID)` · `Pdir_bool` `(bool B)`.

### 12.9 The flags (`Asttypes`)

`Nonrecursive` `:nonrec` · `Recursive` `:rec` · `Upto` `:to` · `Downto` `:downto` · `Private` `:private` ·
`Public` `:public` · `Immutable` `:immutable` · `Mutable` `:mutable` · `Virtual` `:virtual` · `Concrete`
`:concrete` · `Override` `:override` · `Fresh` `:fresh` · `Closed` `:closed` · `Open` `:open` · `Nolabel` `nil` ·
`Labelled` `:l` · `Optional` `:?l` · `Covariant` `:+` · `Contravariant` `:-` · `Bivariant` `:+-` ·
`NoVariance` `nil` · `Injective` `:!` · `NoInjectivity` `nil`.

## 13. Decisions and open questions

### 13.1 Choices made while writing (S1-S29)

- **S1** The forms are the Parsetree's vocabulary (OC8 (a)): heads from constructor names, so the helper writes forms
  directly and the converter of O2 stays near the identity.
- **S2** A trailing `None` is omitted; a non-trailing `None` is `nil`. It makes `(construct Nil)`, `(variant A)` and an
  `if` without `else` the short forms people expect, at the price of a per-form arity rule (§12).
- **S3** Bare symbols only for `[A-Za-z_][A-Za-z0-9_']*` names that are not `true`/`false`/`nil`; everything else is `(name "...")`.
  The alternatives (a table of respellings, as Haskell forms have, 28 of 6,995 names there) were not needed: OCaml's
  operators are all outside the class, and keeping the class tiny means the rule is checkable without a reader
  experiment per token.
- **S4** `()`, `[]`, `::`, `true`, `false` are the keywords `:unit :nil :cons :true :false`.
- **S5** An unknown head is an error; there is no extension point in the forms.
- **S6** Constants keep their source text and suffix.
- **S7** Labels are keywords (`:l`, `:?l`), `nil` for none.
- **S8** Operators are ordinary identifiers; `&&`, `||` are applications, as in the Parsetree.
- **S9** Indexing is the parser's desugaring (`Array.get`, `String.get`, `.%()`), kept.
- **S10** A constrained binding is the Parsetree's: a plain pattern and `pvb_constraint`, not a constrained pattern.
- **S11** The top-level forms are `ocaml/impl`, `ocaml/intf`, `ocaml/toplevel`, `ocaml/error`, `ocaml/typed`,
  `ocaml/lambda`, `ocaml/tokens`.
- **S12** Docstrings are attributes (`ocaml.doc`, `ocaml.text`), not comments.
- **S13** Attributes live in the node's metadata map, not in a field.
- **S14** Items are not wrapped in a `structure-item` record: the item's form carries its positions.
- **S15** Positions are off by default (mode `:lines`), on with `--loc`; the six numbers are line, column, end line,
  end column, offset, end offset.
- **S16** The positions-on forms and the `:lines` forms are one function of each other.
- **S17** Byte columns, as the compiler's `Lexing.position`.
- **S18** `loc_stack` is kept as `:stack` when positions are on.
- **S19** The Typedtree is dumped as annotations (`expr`/`pat` with types), not as a tree (OC20).
- **S20** Lambda is dumped from the bytecode translation (`transl_implementation`), not the native store translation.
- **S21** Lambda identifiers are `Ident.unique_name` strings.
- **S22** Errors are results (`ocaml/error`), the helper exits 0.
- **S23** Value kinds and primitives in Lambda are the compiler's printed text, strings, until O5 needs them structured (OC21).
- **S24** Existential variables of a GADT constructor are `[VAR...]` (names with positions when on).
- **S25** The spec follows the 5.5.0 release's grammar: `include functor` (the Jane Street branch) is not OCaml 5.5.0 and
  is rejected; `module type of`, `let*` operators, effect patterns, `Pmod_apply_unit`, labelled tuples, `Ptyp_functor` and
  `Pexp_struct_item` are 5.5.0's (the survey's O1 row listed `include functor` from memory).
- **S26** The unparenthesised `fun x : t -> e` constraint is the `function`'s `CONSTRAINT`, as OCaml 5.2's Parsetree has it.
- **S27** The class of an expression-level `Pexp_struct_item` is the one node for `let open`, `M.(e)`, `let module`, `let exception`:
  the Parsetree has no separate `Pexp_open`, `Pexp_letmodule` or `Pexp_letexception` in 5.5.0.
- **S28** Names in forms are never split from their locations except through `at` (§10).
- **S29** The head `var` is a special form's name and Arbace's printer writes `(var x)` as `#'x` (it reads back as
  `(var x)`); the spec and the helper write the long form, and the converter's writer must too.

### 13.2 Open questions

They are the survey's proposals OC20 onward ("As built: O1"): the shape of the Typedtree dump (OC20), structured Lambda
primitives (OC21), the comments' representation for `:full` (OC22), the spelling of names that Unicode makes non-bare (OC23),
and `.mll`/`.mly` sources (OC24).

## 14. The round trip and the checks

### 14.1 What O1 checks (`bin/repr-ocaml`)

- `spec-check`: every `forms` block of this file reads with Arbace's reader (stage 0); every example with a
  source equals the helper's output for it (the part its kind names: `expr`, `pat`, `type`, `module`, `modtype`,
  `item`, `sigitem`, `impl`, `intf`); every constructor of `parsetree.mli` and `asttypes.mli` is in §12.
- `samples`: `test/ocaml/samples/` read by the helper without error and the dumps read by Arbace's reader; the typed
  samples typed and lowered.
- `corpus check`: the corpora's counts and the pins of `test/ocaml/corpora.edn`.

### 14.2 What comes after (the plan, survey §11)

O2: the converter and the printer in `:lines`, the round trip forms → OCaml → forms equal and the AST equal to the
helper's on a re-parse of the print, over the corpora; O3: `:full` from the token dump (`ocdump --mode tokens`),
byte for byte; O4: the typed and Lambda dumps over the corpora; the checks are `bin/repr-ocaml roundtrip`/`check`,
heavy, outside O1.

## 15. Sources

- `parsing/parsetree.mli`, `parsing/asttypes.mli`, `parsing/longident.mli`, `parsing/location.mli`,
  `lambda/lambda.mli`, `lambda/printlambda.mli`, `driver/compile_common.mli` of the release tarball
  `ocaml-5.5.0.tar.xz` (`test/ocaml/corpora.edn`'s `:ocaml-5.5.0`, SHA-256 `fcc6ae66...`).
- The host's compiler-libs (`ocaml5-compiler-libs-5.5.0-r0`), the helper's library.
- doc/go/SPEC.md and doc/haskell/SPEC.md (structure and conventions), doc/haskell/HASKELL-NOTES.md (the helper pattern).

## Appendix A: the experiments

Run on the host's OCaml 5.5.0 and stage 0 of main (`target/seed/amd64/arbace`), 2026-10-30; the scripts are
`bin/repr-ocaml` and `tools/ocdump`.

- **E1** The helper over the corpora (sequential, one process a file): stdlib 150 files in 2.7 s, 2 rejected (the two
  `templates/*.template.mli`, macro templates, not OCaml); the compiler's front (parsing, typing, lambda, utils,
  driver) 248 files in 6.0 s, none rejected; the back (asmcomp, bytecomp, middle_end, toplevel, file_formats) 346 files
  in 5.2 s, none; the testsuite 1,862 files in 23 s, 61 rejected (tests of syntax errors, one of the lexer); go-whim's
  whiml 8 files (58,479 lines) in 3.7 s, none.
- **E2** Arbace's reader reads every output (the samples' 20 dumps, positions on and off, the typed and Lambda dumps).
- **E3** Names: of the Parsetree's names in the samples, the operators and keywords-like names are the `(name "...")` ones;
  `true`, `false`, `nil` as identifiers need it too (an identifier named `nil` is legal OCaml).
- **E4** `ocamlfind` is absent and not needed: `ocamlopt -I +compiler-libs ocamlcommon.cmxa` links the helper.
- **E5** `Translmod.transl_implementation` asserts in a native context with no flambda (`Translobj.transl_label_init_flambda`);
  with `Clflags.native_code := false` it gives the bytecode translation (S20).
- **E6** The release tarball has `testsuite/tests`; the GitHub archive of the tag does not (what Alpine builds from).
- **E7** `include functor F` is a syntax error in 5.5.0 (S25).
