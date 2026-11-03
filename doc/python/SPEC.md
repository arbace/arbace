# Python forms: Python modules written in Clojure

Status: draft for the user's review, 2026-11-03: milestone PY1 of the Python crop
([PYTHON-SURVEY.md](PYTHON-SURVEY.md) §11), bound by the survey's decisions PYS1-PYS20 (its section "Decided
(2026-11-03, the user)", every proposal as recommended). The choices made while writing it are S1-S20 (§14).
Nothing in it is a shipped file: the script `bin/repr-python`, the helper's directory `tools/pydump` (its pins and
the spec's checker) and the data of `test/python/` are the first code of the crop, evaluated from the checkout and
out of `gate_key` and the gate (PYS18).

This spec defines the **Python forms**: how every node of a Python 3.14 module, as CPython's parser gives it
(the `ast` module, whose classes are generated from `Parser/Python.asdl` of the release tarball pinned in
`test/python/corpora.edn`), is written as Clojure data in ordinary `.ae` files that Arbace's reader reads, what the
forms keep of the source beyond the `ast`, and how the round trip Python → forms → Python is judged. It is the
repr family's bridge for Python, after repr/java, repr/go, repr/haskell, repr/rust, repr/c, repr/ocaml and
repr/scheme, and follows repr/ocaml's spec in structure ([../ocaml/SPEC.md](../ocaml/SPEC.md)) and the Go and C
forms' spellings of operators ([../go/SPEC.md](../go/SPEC.md), [../c/SPEC.md](../c/SPEC.md)).

Words used throughout:

- **the oracle**: the host's CPython 3.14.10 (Alpine `python3-3.14.10-r1`, pinned by `tools/pydump/PYTHON_VERSION`,
  PYS1 (a)), run isolated (`python3 -I -S -B`).
- **the helper** (PY2): `tools/pydump/pydump.py`, a script on the oracle (no build) that parses one file with
  `ast.parse` and writes the tree **in the forms of this spec**, so the converter is near the identity (PYS3 (a),
  PYS5 (a)); its other modes write the tokens (§11), the errors (§12), `symtable` and the code objects' digests
  (survey §5.1).
- **the converter** (PY2): `arbace.repr.python.convert`, in Arbace, which reads the helper's output, checks it and
  writes `.ae` files; **the printer** (PY2): `arbace.repr.python.print`, forms → Python text.
- **a form**: a list `(head field... option...)`, optionally ending in one metadata map (§1.3).
- **the checklist**: §13, every constructor and field of `Parser/Python.asdl` with its form, checked against the
  pinned tarball's file and against the oracle's `ast` module by `bin/repr-python spec-check`.

Contents: 1 Principles · 2 Names · 3 Literals · 4 Modules · 5 Statements · 6 Expressions · 7 Functions, calls and
parameters · 8 Comprehensions · 9 Patterns · 10 Type parameters · 11 Positions and mode `:full` · 12 Errors ·
13 The checklist · 14 Choices made while writing · 15 The checks · 16 Sources.

## 1. Principles

### 1.1 What the forms are

The forms follow the `ast` one node to one list: the heads are the `ast` classes' names in Arbace's spelling, the
fields are the classes' fields, so `ast.dump` of a file and its forms say the same thing (PYS5 (a)). The `ast` is
**lossy** (the survey's E4): comments, parentheses, `\` continuations, `;`, the choice of `elif` against `else: if`,
the parentheses of `except (A, B):` against PEP 758's `except A, B:`, and the spellings of literals are not in it.
The forms keep, in mode `:lines`, the `ast` plus **the literals' spellings** (§3: `0x1F`, `1_000`, `r"\d"`, the
string prefixes and quotes, the parts of an implicit concatenation, the self-documenting `=` of an f-string) and
`elif` (§5.4); mode `:full` (§11, milestone PY3) adds every node's position and the token stream, so the text comes
back byte for byte.

Python's meaning is (almost) all in this tree plus the scope analysis (survey §1): no type checker's annotations are
part of the forms. `symtable`'s scopes are computed by Arbace (PY5) and checked against the helper's `symtable` mode;
they are not stored in the forms.

### 1.2 Conventions

- **Heads** are the `ast` class's name with each capital after the first preceded by `-` and lower-cased:
  `FunctionDef` is `function-def`, `AsyncFor` is `async-for`, `BinOp` is `bin-op`, `MatchClass` is `match-class`,
  `TemplateStr` is `template-str`, `TypeVarTuple` is `type-var-tuple`. The product types, which have no
  constructor of their own (`comprehension`, `arguments`, `arg`, `keyword`, `alias`, `withitem`, `match_case`),
  take the type's name, `_` as `-`: `match-case`, `withitem`. The top forms are qualified, `python/module`,
  `python/interactive`, `python/expression`, `python/function-type` (§4), as the other crops' are (`ocaml/impl`,
  `go/package`); the heads inside them are not: in **Python context** (inside a top form) every list is a Python
  node and the slot decides its category (an expression, a pattern, a statement), as in Go and OCaml forms.
- **Fields.** A node's fields are either **positional** (after the head, in the order §13 gives, usually the ASDL
  order) or **options** (`:key value` pairs after the positional fields, in the order §13 gives). The required
  fields and the main sequences are positional; the fields that are optional (`?` in the ASDL) or usually empty are
  options. §13 gives every field's slot.
- **Sequences** (ASDL `*`) are vectors `[a b c]`, never omitted when positional: `(call f [])` has no arguments.
- **Absent values.** A positional ASDL option (`?`) that is absent is `nil`; trailing `nil`s are left out
  (`(return)`, `(yield)`, `(slice nil 2)` for `:2`). An option whose value is absent, empty, false or its default
  is left out: `(for x xs [(pass)])` has no `else`. No option's value is a keyword, so a keyword after the
  positional fields always starts an option.
- **Operators** are symbols (§6.2): `+`, `*`, `**`, `<<`, `==`, `is-not`, `not-in`, `and`; the four Python operators
  Arbace's reader cannot read as symbols (`@`, `//`, `^`, `~`) and `|`, `&` are named after their meaning, as Go and
  C forms name them: `mat-mult`, `floor-div`, `bit-xor`, `bit-not`, `bit-or`, `bit-and` (S1).
- **Contexts** (`ctx`: `Load`, `Store`, `Del`) are not written: the slot decides them (a target slot is `Store`, a
  `delete`'s targets `Del`, everything else `Load`), and the converter checks the helper's value against the slot's
  (S15).
- **Booleans** in options are `true` (`:async true`, `:simple false`); integers in options are numbers (`:level 2`).

### 1.3 The metadata map

A node may end in **one map**, and no field or option value is a map. Its keys, each present only when it has a
value:

| key | value | where |
|---|---|---|
| `:pos` | `[lineno col-offset end-lineno end-col-offset]`, as the `ast`'s attributes: lines from 1, columns as **UTF-8 byte offsets** from 0 (E4) | every node with ASDL `attributes`, in mode `:full` only (§11) |
| `:type-comment` | the comment's text after `# type:`, a string | `function-def`, `async-function-def`, `assign`, `for`, `async-for`, `with`, `async-with`, `arg` (§5.8) |
| `:type-ignores` | a vector of `(type-ignore LINE "TAG")` | `python/module` (§4) |
| `:elif` | `true`: this `if` is the `elif` of its parent's `:else` | an `if` alone in an `:else` (§5.4) |
| `:type-comments` | `:rejected`: `ast.parse(type_comments=True)` rejects the file, so it has no type comments (§5.8, S6; queue 174's DECISION (a), decided by the user, 2026-11-03) | `python/module` |
| `:file` | the file's path relative to its corpus root | `python/module` |
| `:mode` | `:lines` or `:full` | `python/module` |
| `:tokens`, `:encoding`, `:bom` | mode `:full`'s token stream and the file's encoding (§11) | `python/module` |

### 1.4 Short and long spellings

Four kinds of node have a **short spelling** besides their list, and both read to the same node:

| node | long | short | when the short one is written |
|---|---|---|---|
| `Name` | `(name x)` | the symbol `x` | always in `:lines`, unless the identifier is not a symbol (§2) |
| `Constant` | `(int "7")`, `(str "a")`, `(const None)` | `7`, `"a"`, `None` | §3: plain decimal integers, plain strings, `None` `True` `False` `...` |
| `alias` | `(alias os.path)` | `os.path` | no `:as` |
| `arg` | `(arg x)` | `x` | no annotation and no type comment |

The converter writes the short spelling wherever it is allowed in mode `:lines`, and the long one in mode `:full`,
where the node carries `:pos` and a symbol, string or number cannot end in a map (§11). A short spelling is never
ambiguous: a symbol in an expression slot is a `Name`, except the four constant symbols, which no `Name` can spell.

```forms
;; py: x = y
(assign [x] y)
```

## 2. Names

- An **identifier** (a `Name`'s `id`, a definition's name, an attribute, a keyword's name, a `global`'s names, a
  pattern's capture) is a symbol, spelled as the `ast` holds it: CPython normalizes identifiers to NFKC (PEP 3131),
  so `ﬁle` is `file` in the `ast` and in the forms; the source spelling is in `:full`'s tokens (S14).
- An identifier the reader would not read back as that symbol (`nil`, `true`, `false` are Python identifiers) is a
  **string** in its slot: `(name "nil")`, `(function-def "true" ...)`, `(attribute x "nil")`. A string in an
  identifier slot is never anything else (S9).
- A **dotted name** (an `import`'s module, an `alias`'s `name`, `import-from`'s module) is one symbol: `os.path`,
  `a.b.c`; `from . import *`'s name is the symbol `*`.

```forms
;; py: true = nil.false
(assign [(name "true")] (attribute (name "nil") "false"))
```

## 3. Literals

`Constant(value, kind)` is one node in the `ast`; in the forms the **spelling** decides the head and the text is
kept, as OCaml's forms keep `Pconst_integer`'s text (PYS5 (a), the survey's §4.2). The value is the spelling's
(the reader computes it; the converter checks that it equals the helper's `value`).

### 3.1 Numbers

| source | form | note |
|---|---|---|
| `0`, `7`, `1234567890123456789012` | `0`, `7`, `1234567890123456789012` | plain decimal (`0` or a digit 1-9 then digits): the number itself (the reader reads a big one as a BigInt, the same value) |
| `0x1F`, `0o17`, `0b101`, `1_000`, `00` | `(int "0x1F")`, `(int "0o17")`, `(int "0b101")`, `(int "1_000")`, `(int "00")` | any other integer spelling, as written (case of `0X`, `0O`, `0B` and of hex digits kept) |
| `1.5`, `1.`, `.5`, `1e3`, `1_0.0_1E-3` | `(float "1.5")`, `(float "1.")`, `(float ".5")`, `(float "1e3")`, `(float "1_0.0_1E-3")` | every float, as written (S5) |
| `2j`, `1.5J`, `1e3j` | `(imag "2j")`, `(imag "1.5J")`, `(imag "1e3j")` | imaginary literals |

A minus sign is not part of a literal: `-1` is `(unary-op - 1)`.

### 3.2 Strings and bytes

A string token is `(str BODY option...)`, a bytes token `(bytes BODY option...)`, where BODY is the text between the
quotes **as written** (escapes, line breaks of a triple-quoted string, everything but the prefix and the quotes), a
Clojure string with Clojure's escapes:

| option | value | default |
|---|---|---|
| `:prefix` | the prefix as written: `"r"`, `"R"`, `"u"`, `"b"`, `"rb"`, `"Br"`, ... (for bytes the `b` is part of it) | `""` for `str`, `"b"` for `bytes` |
| `:quote` | `"'"`, `"\""`, `"'''"` or `"\"\"\""` | `"\""` |

A `str` token with no prefix, the quote `"` and no backslash in its body is written as the bare Clojure string
(S4). `Constant`'s `kind` (`"u"` for a `u` prefix, else `None`) is the prefix's.

```forms
;; py: s = ['a', "b", r"\d", b'\x00', u"x", """two
;; | lines""", "say \"hi\""]
(assign [s] (list [(str "a" :quote "'") "b" (str "\\d" :prefix "r") (bytes "\\x00" :quote "'")
                   (str "x" :prefix "u") (str "two\nlines" :quote "\"\"\"") (str "say \\\"hi\\\"")]))
```

### 3.3 Implicit concatenation

Adjacent string tokens are one `Constant`, `JoinedStr` or `TemplateStr` in the `ast`; the forms keep the parts:
`(str-concat PART PART...)`, each part a `str`, `bytes`, `joined-str` or `template-str` token (S11). The reader
gives the `ast`'s node: the parts' values joined, one `JoinedStr` when any part is an f-string, one `TemplateStr`
when the parts are t-strings (mixing a t-string with another kind is a syntax error, E6). `str-concat` is the one
head that is not an `ast` class.

```forms
;; py: msg = 'a' "b" f"c{d}"
(assign [msg] (str-concat (str "a" :quote "'") "b" (joined-str ["c" (formatted-value d)])))
```

### 3.4 f-strings

`JoinedStr(values)` is `(joined-str [PART...] option...)`, with `:prefix` (default `"f"`: `"F"`, `"rf"`, `"fR"`,
...) and `:quote` (default `"\""`) as for strings. A PART is a literal segment, the Clojure string of its text as
written (escapes and doubled braces `{{` `}}` kept), or a replacement field, `FormattedValue(value, conversion,
format_spec)`:

```
(formatted-value EXPR :conversion r :format-spec [PART...] :debug "x = ")
```

- `:conversion` is the conversion written after `!`: the symbol `s`, `r` or `a` (`conversion` 115, 114, 97);
  absent, the `ast`'s -1.
- `:format-spec` is the spec after `:`, a vector of parts of the same kind (the `ast`'s nested `JoinedStr`, which has
  no prefix or quotes of its own).
- `:debug` is the self-documenting text of `{x = }` (PEP 501's `=`): the expression's text with its spaces and the
  `=`, which the `ast` holds as a `Constant` before the `FormattedValue` (merged with a preceding literal segment)
  and with an implied `conversion` of `r` when neither a conversion nor a spec is written (S12). With `:debug`,
  `:conversion` is the one written, not the implied one.

```forms
;; py: f"a{{b}}{x!r:>{w}}{y = }\n"
(expr (joined-str ["a{{b}}" (formatted-value x :conversion r :format-spec [">" (formatted-value w)])
                   (formatted-value y :debug "y = ") "\\n"]))
```

### 3.5 t-strings (PEP 750)

`TemplateStr(values)` is `(template-str [PART...] option...)`, `:prefix` default `"t"`; its replacement fields are
`Interpolation(value, str, conversion, format_spec)`:

```
(interpolation EXPR "TEXT" :conversion r :format-spec [PART...] :debug "x=")
```

TEXT is the `ast`'s `str`, the expression's source text between `{` and the conversion, spec or `}` (`t"{ x + 1 }"`
gives `" x + 1 "`), a positional field because `ast.dump` compares it.

```forms
;; py: t"hi {name!s:>10} { n + 1 }"
(expr (template-str ["hi " (interpolation name "name" :conversion s :format-spec [">10"])
                     " " (interpolation (bin-op + n 1) " n + 1 ")]))
```

### 3.6 `None`, `True`, `False`, `...`

The symbols `None`, `True`, `False` and `...` (`Constant(None)`, `Constant(True)`, `Constant(False)`,
`Constant(Ellipsis)`); long `(const None)`, `(const ...)`. In a `match-singleton` they are the pattern's value.

```forms
;; py: x = (None, True, False, ...)
(assign [x] (tuple [None True False ...]))
```

## 4. Modules

```
(python/module [STMT...] {:file "pkg/mod.py" :mode :lines :type-ignores [(type-ignore 3 "[attr]")]})
(python/interactive [STMT...])                     ; ast.parse(mode="single"): the REPL's input
(python/expression EXPR)                           ; mode="eval": eval()'s input
(python/function-type [EXPR...] EXPR)              ; mode="func_type": a signature type comment
```

One file is one `python/module`, one `.ae` per `.py`; a package is a directory, its `__init__.py` an `__init__.ae`
(survey §4.3). `TypeIgnore(lineno, tag)` (a `# type: ignore[tag]` comment, with `type_comments=True`) is
`(type-ignore LINE "TAG")` in the module's `:type-ignores`. Docstrings are ordinary `(expr "...")` statements.

```forms
;; py: """Doc."""
;; | import os
(expr "Doc.")
(import [os])
```

## 5. Statements

### 5.1 Simple statements

| source | form |
|---|---|
| `x = y = v` | `(assign [x y] v)` |
| `x += 1` | `(aug-assign x + 1)` |
| `x: int = 0`, `x: int`, `(x): int` | `(ann-assign x int 0)`, `(ann-assign x int)`, `(ann-assign x int :simple false)` |
| `del a, b[0]` | `(delete [a (subscript b 0)])` |
| `return`, `return v` | `(return)`, `(return v)` |
| `raise`, `raise E`, `raise E from c` | `(raise)`, `(raise E)`, `(raise E :from c)` |
| `assert t`, `assert t, m` | `(assert t)`, `(assert t m)` |
| `pass`, `break`, `continue` | `(pass)`, `(break)`, `(continue)` |
| `global a, b`, `nonlocal c` | `(global [a b])`, `(nonlocal [c])` |
| an expression statement | `(expr e)` |
| `type X[T] = list[T]` | `(type-alias X (subscript list T) :type-params [(type-var T)])` |

`AnnAssign`'s `simple` is 1 when the target is a bare name, 0 for a parenthesized name and for any other target; the
forms write `:simple false` only for a parenthesized name (the one case the target does not decide).

```forms
;; py: x: int = 0
(ann-assign x int 0)
;; py: (y): str
(ann-assign y str :simple false)
;; py: a.b[c] -= 2 ** n
(aug-assign (subscript (attribute a b) c) - (bin-op ** 2 n))
```

### 5.2 Imports

`Import(names)` is `(import [ALIAS...])`, `ImportFrom(module, names, level)` is `(import-from MODULE [ALIAS...]
:level N)` with `nil` for no module (`from . import x`) and `:level` left out at 0; `alias(name, asname)` is
`(alias NAME :as ASNAME)`, short the name alone.

```forms
;; py: import os.path as p, sys
(import [(alias os.path :as p) sys])
;; py: from .. import (a as b, c)
(import-from nil [(alias a :as b) c] :level 2)
;; py: from x.y import *
(import-from x.y [*])
```

### 5.3 Loops

`(for TARGET ITER [BODY] :else [ORELSE])`, `(async-for ...)`, `(while TEST [BODY] :else [ORELSE])`.

```forms
;; py: for i, x in enumerate(xs):
;; |     continue
;; | else:
;; |     pass
(for (tuple [i x]) (call enumerate [xs]) [(continue)] :else [(pass)])
;; py: while n:
;; |     n -= 1
(while n [(aug-assign n - 1)])
```

### 5.4 `if` and `elif`

`(if TEST [BODY] :else [ORELSE])`. An `elif` is, as in the `ast`, an `if` alone in its parent's `:else`; the forms
mark it `{:elif true}` so that the print writes `elif` again (S7; the `ast` is the same for `else:` followed by an
indented `if`, and so are the code objects without positions).

```forms
;; py: if a:
;; |     pass
;; | elif b:
;; |     pass
;; | else:
;; |     pass
(if a [(pass)] :else [(if b [(pass)] :else [(pass)] {:elif true})])
```

### 5.5 `with`

`(with [ITEM...] [BODY])`, `(async-with ...)`; `withitem(context_expr, optional_vars)` is `(withitem EXPR TARGET)`,
the target left out when there is no `as`.

```forms
;; py: with open(p) as f, lock:
;; |     pass
(with [(withitem (call open [p]) f) (withitem lock)] [(pass)])
```

### 5.6 `try` and `except*`

`(try [BODY] [HANDLER...] :else [ORELSE] :finally [FINALBODY])`, and `(try-star ...)` for `except*` (PEP 654);
`ExceptHandler(type, name, body)` is `(except-handler [BODY] :type EXPR :name NAME)`. PEP 758's `except A, B:` is the
`ast`'s `Tuple`, as `except (A, B):` is (E4); the parentheses are `:full`'s (S10).

```forms
;; py: try:
;; |     pass
;; | except ValueError, TypeError:
;; |     pass
;; | except OSError as e:
;; |     raise
;; | finally:
;; |     pass
(try [(pass)] [(except-handler [(pass)] :type (tuple [ValueError TypeError]))
               (except-handler [(raise)] :type OSError :name e)]
     :finally [(pass)])
;; py: try:
;; |     pass
;; | except* E:
;; |     pass
(try-star [(pass)] [(except-handler [(pass)] :type E)])
```

### 5.7 `match`

`(match SUBJECT [CASE...])`, `match_case(pattern, guard, body)` is `(match-case PATTERN [BODY] :guard EXPR)`; the
patterns are §9's.

```forms
;; py: match p:
;; |     case [1, *rest] if rest:
;; |         pass
(match p [(match-case (match-sequence [(match-value 1) (match-star rest)]) [(pass)] :guard rest)])
```

### 5.8 Type comments

With `type_comments=True` the `ast` holds `# type:` comments in a `type_comment` field of `FunctionDef`,
`AsyncFunctionDef`, `Assign`, `For`, `AsyncFor`, `With`, `AsyncWith` and `arg`, and `# type: ignore` comments as the
module's `type_ignores`. The forms keep them in metadata (`:type-comment`, `:type-ignores`), never as fields.
`ast.parse(type_comments=True)` rejects some files the plain parse accepts (a comment that only looks like a type
comment, as `# type: 1 byte` inside an expression in `Lib/test/test_tarfile.py`): the helper parses plainly for the
tree and the acceptance, then with `type_comments=True` for the metadata alone, and a file the second parse rejects
has no type comments and says so (`:type-comments :rejected` in its module's map; S6: queue 174's DECISION (a), decided by the user, 2026-11-03).

```forms
;; py: x = [] # type: list[int]
(assign [x] (list []) {:type-comment "list[int]"})
```

## 6. Expressions

### 6.1 Atoms, containers, attributes and subscripts

| source | form |
|---|---|
| `x` | `x` (`(name x)`) |
| `[a, b]`, `(a, b)`, `{a, b}` | `(list [a b])`, `(tuple [a b])`, `(set [a b])` |
| `{k: v, **d}` | `(dict [k nil] [v d])`: the keys and the values, `nil` for a `**` entry, as the `ast`'s two lists |
| `a.b` | `(attribute a b)` |
| `a[i]`, `a[i, j]` | `(subscript a i)`, `(subscript a (tuple [i j]))` |
| `a[1:2]`, `a[::3]`, `a[:]` | `(subscript a (slice 1 2))`, `(subscript a (slice nil nil 3))`, `(subscript a (slice))` |
| `*xs` | `(starred xs)` |
| `(x := f())` | `(named-expr x (call f []))` |
| `a if t else b` | `(if-exp t a b)`: test, body, orelse |
| `lambda x: x` | `(lambda (arguments :args [x]) x)` |
| `await e`, `yield`, `yield e`, `yield from e` | `(await e)`, `(yield)`, `(yield e)`, `(yield-from e)` |

```forms
;; py: v = {k: [a[1:], (b, *c)], **d}
(assign [v] (dict [k nil] [(list [(subscript a (slice 1)) (tuple [b (starred c)])]) d]))
```

### 6.2 Operators

`(bin-op OP LEFT RIGHT)`, `(unary-op OP OPERAND)`, `(bool-op OP [VALUE...])`, `(compare LEFT [OP...]
[COMPARATOR...])`; the operator comes first in `bin-op`, as the survey's example has it (S2). The operators:

| `ast` | Python | form | | `ast` | Python | form |
|---|---|---|---|---|---|---|
| `Add` | `+` | `+` | | `Invert` | `~` | `bit-not` |
| `Sub` | `-` | `-` | | `Not` | `not` | `not` |
| `Mult` | `*` | `*` | | `UAdd` | `+` | `+` |
| `MatMult` | `@` | `mat-mult` | | `USub` | `-` | `-` |
| `Div` | `/` | `/` | | `And` | `and` | `and` |
| `Mod` | `%` | `%` | | `Or` | `or` | `or` |
| `Pow` | `**` | `**` | | `Eq`, `NotEq` | `==`, `!=` | `==`, `!=` |
| `LShift`, `RShift` | `<<`, `>>` | `<<`, `>>` | | `Lt`, `LtE` | `<`, `<=` | `<`, `<=` |
| `BitOr` | `\|` | `bit-or` | | `Gt`, `GtE` | `>`, `>=` | `>`, `>=` |
| `BitXor` | `^` | `bit-xor` | | `Is`, `IsNot` | `is`, `is not` | `is`, `is-not` |
| `BitAnd` | `&` | `bit-and` | | `In`, `NotIn` | `in`, `not in` | `in`, `not-in` |
| `FloorDiv` | `//` | `floor-div` | | | | |

The slot decides which table a symbol is from: `+` in a `unary-op` is `UAdd`, in a `bin-op` or `aug-assign` `Add`.
A chain `a < b <= c` is one `compare`; `a and b and c` one `bool-op` (the `ast`'s nesting is kept as it is: `(a and
b) and c` is two).

```forms
;; py: r = -a // b @ c ^ ~d | (e & f) % g
(assign [r] (bin-op bit-or (bin-op bit-xor (bin-op floor-div (unary-op - a) (bin-op mat-mult b c)) (unary-op bit-not d))
                    (bin-op % (bin-op bit-and e f) g)))
;; py: ok = not (x is not None and 0 < y <= 9 or z not in s)
(assign [ok] (unary-op not (bool-op or [(bool-op and [(compare x [is-not] [None]) (compare 0 [< <=] [y 9])])
                                        (compare z [not-in] [s])])))
```

## 7. Functions, calls and parameters

### 7.1 Definitions

```
(function-def NAME ARGUMENTS [BODY] :decorators [EXPR...] :returns EXPR :type-params [TYPE-PARAM...])
(async-function-def ...)
(class-def NAME [BODY] :bases [EXPR...] :keywords [KEYWORD...] :decorators [EXPR...] :type-params [...])
```

`arguments(posonlyargs, args, vararg, kwonlyargs, kw_defaults, kwarg, defaults)` is `(arguments :posonly [ARG...]
:args [ARG...] :vararg ARG :kwonly [ARG...] :kw-defaults [EXPR-OR-NIL...] :kwarg ARG :defaults [EXPR...])`, every
option left out when empty, so `def f():` has `(arguments)`. As in the `ast`, `:defaults` are the last defaults of
`:posonly` and `:args` together, and `:kw-defaults` has one entry per `:kwonly`, `nil` for none; it is left out when
every entry is `nil` (the reader fills it with `nil`s, S8). `arg(arg, annotation, type_comment)` is `(arg NAME
ANNOTATION)`, short the name alone.

```forms
;; py: @cache
;; | def f[T](a: T, /, b=1, *c, d, e=2, **g) -> T:
;; |     return a
(function-def f (arguments :posonly [(arg a T)] :args [b] :vararg c :kwonly [d e] :kw-defaults [nil 2] :kwarg g
                           :defaults [1])
              [(return a)] :decorators [cache] :returns T :type-params [(type-var T)])
;; py: class C[T](B, metaclass=M):
;; |     x = 1
(class-def C [(assign [x] 1)] :bases [B] :keywords [(keyword metaclass M)] :type-params [(type-var T)])
;; py: async def g():
;; |     async with a as b:
;; |         async for x in b:
;; |             await x
(async-function-def g (arguments) [(async-with [(withitem a b)] [(async-for x b [(expr (await x))])])])
```

### 7.2 Calls

`(call FUNC [ARG...] :keywords [KEYWORD...])`; `keyword(arg, value)` is `(keyword NAME VALUE)`, `(keyword nil VALUE)`
for `**value`; a `*args` argument is a `starred` among the arguments. The `ast` keeps positional and keyword
arguments apart, so `f(k=1, *a)` and `f(*a, k=1)` are one node (S10).

```forms
;; py: f(x, *a, k=1, **kw)
(expr (call f [x (starred a)] :keywords [(keyword k 1) (keyword nil kw)]))
```

## 8. Comprehensions

`(list-comp ELT [GEN...])`, `(set-comp ELT [GEN...])`, `(dict-comp KEY VALUE [GEN...])`, `(generator-exp ELT
[GEN...])`; `comprehension(target, iter, ifs, is_async)` is `(comprehension TARGET ITER :ifs [EXPR...] :async
true)`.

```forms
;; py: async def h(d, g):
;; |     return {k: v for k, v in d.items() if v async for _ in g}
(async-function-def h (arguments :args [d g])
                    [(return (dict-comp k v [(comprehension (tuple [k v]) (call (attribute d items) []) :ifs [v])
                                             (comprehension _ g :async true)]))])
;; py: s = sum(x * x for x in xs)
(assign [s] (call sum [(generator-exp (bin-op * x x) [(comprehension x xs)])]))
```

## 9. Patterns

| `ast` | source | form |
|---|---|---|
| `MatchValue(value)` | `1`, `-1`, `a.b`, `"s"` | `(match-value 1)`, `(match-value (unary-op - 1))`, `(match-value (attribute a b))` |
| `MatchSingleton(value)` | `None`, `True`, `False` | `(match-singleton None)` |
| `MatchSequence(patterns)` | `[a, *r]`, `(a, b)` | `(match-sequence [(match-as nil a) (match-star r)])` |
| `MatchMapping(keys, patterns, rest)` | `{"k": v, **r}` | `(match-mapping ["k"] [(match-as nil v)] :rest r)` |
| `MatchClass(cls, patterns, kwd_attrs, kwd_patterns)` | `P(x, y=0)` | `(match-class P [(match-as nil x)] :kwd-attrs [y] :kwd-patterns [(match-value 0)])` |
| `MatchStar(name)` | `*r`, `*_` | `(match-star r)`, `(match-star)` |
| `MatchAs(pattern, name)` | `_`, `x`, `[a] as s` | `(match-as)`, `(match-as nil x)`, `(match-as (match-sequence [(match-as nil a)]) s)` |
| `MatchOr(patterns)` | `1 \| 2` | `(match-or [(match-value 1) (match-value 2)])` |

```forms
;; py: match cmd:
;; |     case {"op": "go", **rest}:
;; |         pass
;; |     case Point(x=0, y=_) | [None, True, *_] as whole:
;; |         pass
;; |     case _:
;; |         pass
(match cmd [(match-case (match-mapping ["op"] [(match-value "go")] :rest rest) [(pass)])
            (match-case (match-as (match-or [(match-class Point [] :kwd-attrs [x y] :kwd-patterns [(match-value 0) (match-as)])
                                             (match-sequence [(match-singleton None) (match-singleton True) (match-star)])])
                                  whole)
                        [(pass)])
            (match-case (match-as) [(pass)])])
```

## 10. Type parameters (PEP 695, PEP 696)

`(type-var NAME :bound EXPR :default EXPR)`, `(param-spec NAME :default EXPR)`, `(type-var-tuple NAME :default
EXPR)`, in a definition's or a `type-alias`'s `:type-params`; constraints `T: (int, str)` are a `tuple` bound.

```forms
;; py: type Pair[*Ts, T: (int, str) = int, **P = [int]] = tuple[T, T]
(type-alias Pair (subscript tuple (tuple [T T]))
            :type-params [(type-var-tuple Ts) (type-var T :bound (tuple [int str]) :default int)
                          (param-spec P :default (list [int]))])
```

## 11. Positions and mode `:full`

Mode `:lines` (PY2) carries no positions: its round trip is judged at the forms, `ast.dump` without attributes and
the code objects without positions (survey §5.2, levels 1-3). Mode `:full` (PY3, PYS6 (a)) adds:

- **`:pos`** on every node with ASDL `attributes` (`stmt`, `expr`, `excepthandler`, `arg`, `keyword`, `alias`,
  `pattern`, `type_param`): `[lineno col-offset end-lineno end-col-offset]`, columns in UTF-8 bytes. Such a node is
  written in its long spelling (§1.4): `(name x {:pos [1 0 1 1]})`, `(int "0x1F" {:pos [1 14 1 18]})`, `(const None
  {:pos ...})`, `(str "a" :quote "'" {:pos ...})`. Nodes without attributes (`arguments`, `comprehension`,
  `withitem`, `match-case`, the operators) carry none, as in the `ast`.
- **`:tokens`** in the module's map: the token stream of `tokenize.tokenize` (E5), a vector of `[KIND "TEXT"]`, KIND
  the token type's name as a keyword (`:name`, `:op`, `:number`, `:string`, `:fstring-start`, `:fstring-middle`,
  `:fstring-end`, `:tstring-start`, `:tstring-middle`, `:tstring-end`, `:comment`, `:newline`, `:nl`, `:indent`,
  `:dedent`, `:endmarker`) and `:ws` for the text between two tokens (spaces, tabs, form feeds, `\` continuations).
  TEXT is the source's slice, not `tokenize`'s string (which undoubles `{{` in an `fstring-middle`: probe of
  2026-11-03, `f"a{{b}}"` gives `'a{'` for the source's `a{{`), so the concatenation of every TEXT is the decoded
  file, exactly (a property of the representation the helper checks on each file).
- **`:encoding`** (the coding cookie's encoding, PEP 263, default `"utf-8"`) and **`:bom`** `true` for a UTF-8 BOM:
  the print encodes the text back to the original bytes.

The print of `:full` is the tokens' text, encoded; the forms' positions let the converter check that every node lies
on its tokens and let the printer place a changed node (PY3).

## 12. Errors

A file `ast.parse` rejects is read to one form, `(python/error CLASS MSG option...)`, with CLASS the exception's
class (`SyntaxError`, `IndentationError`, `TabError`), MSG its `msg`, and the options `:pos [lineno offset end-lineno
end-offset]` (as the exception holds them: lines from 1, offsets in characters from 1; absent when `None`, as for
`source code string cannot contain null bytes`) and `:text` (the offending line). The read level judges a rejected
file on CLASS, MSG and `:pos` (survey §5.3).

```forms
(python/error SyntaxError "'(' was never closed" :pos [1 5 1 0] :text "x = (\n")
(python/error TabError "inconsistent use of tabs and spaces in indentation" :pos [3 1 3 0] :text "        pass\n")
(python/error SyntaxError "source code string cannot contain null bytes")
```

The errors of the compiler and of `symtable` (`return` outside a function, `nonlocal` at module level, a pattern
binding different names in its alternatives, E6) are not the reader's: `ast.parse` accepts those files, the forms
are read, and the error is eval's (PY5), judged as a run.

## 13. The checklist

Every type and constructor of `Parser/Python.asdl` (3.14.10, the pinned tarball), each field with its slot: `N` the
N-th positional field, `:key` an option, `meta:key` a key of the metadata map (§1.3), `spelling` the literal's
spelling (§3), `dropped` not written (decided by the slot, checked). A row *attributes* gives a type's position
attributes. `bin/repr-python spec-check` reads this table and `Python.asdl` and fails on a type, constructor or field
of either that the other lacks, on a slot that is not of these kinds, on positional slots that are not 1..N, and on
a head used twice within a category; and it checks the oracle's `ast` module against the same file (every class's
`_fields` and `_attributes`).

| type | constructor | head | fields |
|---|---|---|---|
| `mod` | `Module` | `python/module` | `body→1`, `type_ignores→meta:type-ignores` |
| `mod` | `Interactive` | `python/interactive` | `body→1` |
| `mod` | `Expression` | `python/expression` | `body→1` |
| `mod` | `FunctionType` | `python/function-type` | `argtypes→1`, `returns→2` |
| `stmt` | `FunctionDef` | `function-def` | `name→1`, `args→2`, `body→3`, `decorator_list→:decorators`, `returns→:returns`, `type_comment→meta:type-comment`, `type_params→:type-params` |
| `stmt` | `AsyncFunctionDef` | `async-function-def` | `name→1`, `args→2`, `body→3`, `decorator_list→:decorators`, `returns→:returns`, `type_comment→meta:type-comment`, `type_params→:type-params` |
| `stmt` | `ClassDef` | `class-def` | `name→1`, `bases→:bases`, `keywords→:keywords`, `body→2`, `decorator_list→:decorators`, `type_params→:type-params` |
| `stmt` | `Return` | `return` | `value→1` |
| `stmt` | `Delete` | `delete` | `targets→1` |
| `stmt` | `Assign` | `assign` | `targets→1`, `value→2`, `type_comment→meta:type-comment` |
| `stmt` | `TypeAlias` | `type-alias` | `name→1`, `type_params→:type-params`, `value→2` |
| `stmt` | `AugAssign` | `aug-assign` | `target→1`, `op→2`, `value→3` |
| `stmt` | `AnnAssign` | `ann-assign` | `target→1`, `annotation→2`, `value→3`, `simple→:simple` |
| `stmt` | `For` | `for` | `target→1`, `iter→2`, `body→3`, `orelse→:else`, `type_comment→meta:type-comment` |
| `stmt` | `AsyncFor` | `async-for` | `target→1`, `iter→2`, `body→3`, `orelse→:else`, `type_comment→meta:type-comment` |
| `stmt` | `While` | `while` | `test→1`, `body→2`, `orelse→:else` |
| `stmt` | `If` | `if` | `test→1`, `body→2`, `orelse→:else` |
| `stmt` | `With` | `with` | `items→1`, `body→2`, `type_comment→meta:type-comment` |
| `stmt` | `AsyncWith` | `async-with` | `items→1`, `body→2`, `type_comment→meta:type-comment` |
| `stmt` | `Match` | `match` | `subject→1`, `cases→2` |
| `stmt` | `Raise` | `raise` | `exc→1`, `cause→:from` |
| `stmt` | `Try` | `try` | `body→1`, `handlers→2`, `orelse→:else`, `finalbody→:finally` |
| `stmt` | `TryStar` | `try-star` | `body→1`, `handlers→2`, `orelse→:else`, `finalbody→:finally` |
| `stmt` | `Assert` | `assert` | `test→1`, `msg→2` |
| `stmt` | `Import` | `import` | `names→1` |
| `stmt` | `ImportFrom` | `import-from` | `module→1`, `names→2`, `level→:level` |
| `stmt` | `Global` | `global` | `names→1` |
| `stmt` | `Nonlocal` | `nonlocal` | `names→1` |
| `stmt` | `Expr` | `expr` | `value→1` |
| `stmt` | `Pass` | `pass` | |
| `stmt` | `Break` | `break` | |
| `stmt` | `Continue` | `continue` | |
| `stmt` | *attributes* | | `lineno→meta:pos`, `col_offset→meta:pos`, `end_lineno→meta:pos`, `end_col_offset→meta:pos` |
| `expr` | `BoolOp` | `bool-op` | `op→1`, `values→2` |
| `expr` | `NamedExpr` | `named-expr` | `target→1`, `value→2` |
| `expr` | `BinOp` | `bin-op` | `left→2`, `op→1`, `right→3` |
| `expr` | `UnaryOp` | `unary-op` | `op→1`, `operand→2` |
| `expr` | `Lambda` | `lambda` | `args→1`, `body→2` |
| `expr` | `IfExp` | `if-exp` | `test→1`, `body→2`, `orelse→3` |
| `expr` | `Dict` | `dict` | `keys→1`, `values→2` |
| `expr` | `Set` | `set` | `elts→1` |
| `expr` | `ListComp` | `list-comp` | `elt→1`, `generators→2` |
| `expr` | `SetComp` | `set-comp` | `elt→1`, `generators→2` |
| `expr` | `DictComp` | `dict-comp` | `key→1`, `value→2`, `generators→3` |
| `expr` | `GeneratorExp` | `generator-exp` | `elt→1`, `generators→2` |
| `expr` | `Await` | `await` | `value→1` |
| `expr` | `Yield` | `yield` | `value→1` |
| `expr` | `YieldFrom` | `yield-from` | `value→1` |
| `expr` | `Compare` | `compare` | `left→1`, `ops→2`, `comparators→3` |
| `expr` | `Call` | `call` | `func→1`, `args→2`, `keywords→:keywords` |
| `expr` | `FormattedValue` | `formatted-value` | `value→1`, `conversion→:conversion`, `format_spec→:format-spec` |
| `expr` | `Interpolation` | `interpolation` | `value→1`, `str→2`, `conversion→:conversion`, `format_spec→:format-spec` |
| `expr` | `JoinedStr` | `joined-str` | `values→1` |
| `expr` | `TemplateStr` | `template-str` | `values→1` |
| `expr` | `Constant` | `int` `float` `imag` `str` `bytes` `const` | `value→spelling`, `kind→:prefix` |
| `expr` | `Attribute` | `attribute` | `value→1`, `attr→2`, `ctx→dropped` |
| `expr` | `Subscript` | `subscript` | `value→1`, `slice→2`, `ctx→dropped` |
| `expr` | `Starred` | `starred` | `value→1`, `ctx→dropped` |
| `expr` | `Name` | `name` | `id→1`, `ctx→dropped` |
| `expr` | `List` | `list` | `elts→1`, `ctx→dropped` |
| `expr` | `Tuple` | `tuple` | `elts→1`, `ctx→dropped` |
| `expr` | `Slice` | `slice` | `lower→1`, `upper→2`, `step→3` |
| `expr` | *attributes* | | `lineno→meta:pos`, `col_offset→meta:pos`, `end_lineno→meta:pos`, `end_col_offset→meta:pos` |
| `expr_context` | `Load` | | |
| `expr_context` | `Store` | | |
| `expr_context` | `Del` | | |
| `boolop` | `And` | `and` | |
| `boolop` | `Or` | `or` | |
| `operator` | `Add` | `+` | |
| `operator` | `Sub` | `-` | |
| `operator` | `Mult` | `*` | |
| `operator` | `MatMult` | `mat-mult` | |
| `operator` | `Div` | `/` | |
| `operator` | `Mod` | `%` | |
| `operator` | `Pow` | `**` | |
| `operator` | `LShift` | `<<` | |
| `operator` | `RShift` | `>>` | |
| `operator` | `BitOr` | `bit-or` | |
| `operator` | `BitXor` | `bit-xor` | |
| `operator` | `BitAnd` | `bit-and` | |
| `operator` | `FloorDiv` | `floor-div` | |
| `unaryop` | `Invert` | `bit-not` | |
| `unaryop` | `Not` | `not` | |
| `unaryop` | `UAdd` | `+` | |
| `unaryop` | `USub` | `-` | |
| `cmpop` | `Eq` | `==` | |
| `cmpop` | `NotEq` | `!=` | |
| `cmpop` | `Lt` | `<` | |
| `cmpop` | `LtE` | `<=` | |
| `cmpop` | `Gt` | `>` | |
| `cmpop` | `GtE` | `>=` | |
| `cmpop` | `Is` | `is` | |
| `cmpop` | `IsNot` | `is-not` | |
| `cmpop` | `In` | `in` | |
| `cmpop` | `NotIn` | `not-in` | |
| `comprehension` | `comprehension` | `comprehension` | `target→1`, `iter→2`, `ifs→:ifs`, `is_async→:async` |
| `excepthandler` | `ExceptHandler` | `except-handler` | `type→:type`, `name→:name`, `body→1` |
| `excepthandler` | *attributes* | | `lineno→meta:pos`, `col_offset→meta:pos`, `end_lineno→meta:pos`, `end_col_offset→meta:pos` |
| `arguments` | `arguments` | `arguments` | `posonlyargs→:posonly`, `args→:args`, `vararg→:vararg`, `kwonlyargs→:kwonly`, `kw_defaults→:kw-defaults`, `kwarg→:kwarg`, `defaults→:defaults` |
| `arg` | `arg` | `arg` | `arg→1`, `annotation→2`, `type_comment→meta:type-comment` |
| `arg` | *attributes* | | `lineno→meta:pos`, `col_offset→meta:pos`, `end_lineno→meta:pos`, `end_col_offset→meta:pos` |
| `keyword` | `keyword` | `keyword` | `arg→1`, `value→2` |
| `keyword` | *attributes* | | `lineno→meta:pos`, `col_offset→meta:pos`, `end_lineno→meta:pos`, `end_col_offset→meta:pos` |
| `alias` | `alias` | `alias` | `name→1`, `asname→:as` |
| `alias` | *attributes* | | `lineno→meta:pos`, `col_offset→meta:pos`, `end_lineno→meta:pos`, `end_col_offset→meta:pos` |
| `withitem` | `withitem` | `withitem` | `context_expr→1`, `optional_vars→2` |
| `match_case` | `match_case` | `match-case` | `pattern→1`, `guard→:guard`, `body→2` |
| `pattern` | `MatchValue` | `match-value` | `value→1` |
| `pattern` | `MatchSingleton` | `match-singleton` | `value→1` |
| `pattern` | `MatchSequence` | `match-sequence` | `patterns→1` |
| `pattern` | `MatchMapping` | `match-mapping` | `keys→1`, `patterns→2`, `rest→:rest` |
| `pattern` | `MatchClass` | `match-class` | `cls→1`, `patterns→2`, `kwd_attrs→:kwd-attrs`, `kwd_patterns→:kwd-patterns` |
| `pattern` | `MatchStar` | `match-star` | `name→1` |
| `pattern` | `MatchAs` | `match-as` | `pattern→1`, `name→2` |
| `pattern` | `MatchOr` | `match-or` | `patterns→1` |
| `pattern` | *attributes* | | `lineno→meta:pos`, `col_offset→meta:pos`, `end_lineno→meta:pos`, `end_col_offset→meta:pos` |
| `type_ignore` | `TypeIgnore` | `type-ignore` | `lineno→1`, `tag→2` |
| `type_param` | `TypeVar` | `type-var` | `name→1`, `bound→:bound`, `default_value→:default` |
| `type_param` | `ParamSpec` | `param-spec` | `name→1`, `default_value→:default` |
| `type_param` | `TypeVarTuple` | `type-var-tuple` | `name→1`, `default_value→:default` |
| `type_param` | *attributes* | | `lineno→meta:pos`, `col_offset→meta:pos`, `end_lineno→meta:pos`, `end_col_offset→meta:pos` |

The heads beyond the `ast`: `str-concat` (§3.3) and `python/error` (§12); the short spellings of §1.4.

## 14. Choices made while writing (S1-S20)

- **S1 Operators.** The survey's example spelled `//` and `@`, which Arbace's reader does not read as symbols
  (`(read-string "//")` is "Invalid token", `@` and `~` are reader macros, `^` metadata: probed on stage 0,
  2026-11-03); they and the bit operators take the Go and C forms' names (`floor-div`, `mat-mult`, `bit-xor`,
  `bit-not`, `bit-or`, `bit-and`); the two-word operators are `is-not` and `not-in`.
- **S2 Slots.** Positional fields for what every node of the kind has, options for the rest, both in §13's order;
  `bin-op`'s operator first (the survey's example), `aug-assign`'s second (the source's order).
- **S3 Short spellings** (§1.4) in `:lines`; the long ones are always valid and are `:full`'s.
- **S4 Strings** keep their body as written with `:prefix` and `:quote`; a plain double-quoted string without
  backslashes is the bare Clojure string. The value is computed, never stored, so a string has one spelling.
- **S5 Numbers.** Plain decimal integers bare; every other integer, every float and every imaginary literal as
  `(int|float|imag "TEXT")` (a bare float would be printed back by Clojure's rules, `1.` as `1.0`).
- **S6 Type comments** by a second parse (§5.8): `type_comments=True` rejects one file of `Lib/test` that CPython
  runs (measured, 2026-11-03: 1 of 1,869 files of `Lib/`, `test_tarfile.py`'s `# type: 1 byte`), so it cannot be
  the read level's parse.
- **S7 `elif`** kept as `{:elif true}`: the `ast` cannot tell it from `else: if`, but the print should not rewrite
  every `elif` (the `ast` and the code objects without positions are equal either way).
- **S8 `arguments`** by options, `:kw-defaults` left out when all `nil`; `dict`'s keys and values as two vectors,
  as the `ast`'s.
- **S9 Identifiers** that read as other data (`nil`, `true`, `false`) are strings in their slot.
- **S10 Dropped spellings** (the `ast` is equal and so are the code objects without positions): parentheses (of
  tuples, of `with` items, of `except` types, of a generator expression as the only argument), `[` against `(` in a
  sequence pattern, the order of `*args` among keyword arguments in a call, `a[::]` against `a[:]`; `:full`'s tokens
  keep them.
- **S11 Implicit concatenation** kept as `str-concat` (the survey's §4.2).
- **S12 f-string `=`** kept as `:debug`, with the conversion as written.
- **S13 `Constant.kind`** (`"u"`) is the `u` prefix, not a field of its own.
- **S14 NFKC.** Identifiers as the `ast` holds them (normalized); the original spelling in `:full`'s tokens.
- **S15 Contexts** dropped and checked (the survey's §4.2).
- **S16 One form per file**, `python/module`, the module's facts in its map (`:file`, `:mode`, `:type-ignores`,
  `:tokens`).
- **S17 Errors** as one `python/error` form with the exception's fields, offsets in characters as CPython gives
  them (not the `ast`'s bytes).
- **S18 Docstrings** are statements, as in the `ast`.
- **S19 Product types** take their type's name as head (`withitem`, `match-case`, `arguments`).
- **S20 Python context.** Inner heads unqualified; only the top forms and `python/error` are `python/`-qualified.

## 15. The checks

PY1's, all light (no Arbace build, no stage 0 build; stage 0 only reads data):

- `bin/repr-python spec-check`: the checklist against `Parser/Python.asdl` of the pinned tarball and against the
  oracle's `ast` module (§13); every `forms` block of this file read by Arbace's reader; every worked example's
  Python (`;; py:` lines) parsed and compiled by the oracle (a valid program, not only a parse), and every `ast`
  class in its tree (but `Name`, `Constant`, `alias`, `arg`, the contexts and the operators, which have short
  spellings or are symbols, and the format specs, which are vectors) present as its head in the example's forms.
  The examples are checked for their heads, not yet node for node: the helper and the converter that would
  compare a form with its source's tree are PY2's (OCaml's spec-check compares them, at O1, with its helper).
- `bin/repr-python pins --check`: the oracle is the pinned one (`tools/pydump/PYTHON_VERSION`: the version,
  `sys.version`, the installed apk and its checksum for the host's architecture), the tarball and the site-packages
  apks match their SHA-256 (`test/python/corpora.edn`, `tools/pydump/apks.sha256`), the reference files of the
  tarball match theirs, and every corpus has the recorded counts.
- `bin/repr-python samples`: every file of `test/python/samples/` parsed by the oracle (plain and with
  `type_comments=True`), compiled, and the samples together covering every node class of the oracle's `ast`
  (`Python.asdl`'s constructors, by the check above) but `Interactive`, `Expression` and `FunctionType`: 110 of
  110 (the six samples, 256 lines, are deterministic programs, their stdout the same under `PYTHONHASHSEED` 0 and
  7, for the eval levels' references at PY4).

PY2's (the survey's "As built: PY2"):

- **The helper** `tools/pydump/pydump.py tree` writes these forms, plus one thing the forms do not hold: a `Name`,
  `Attribute`, `Subscript`, `Starred`, `List` or `Tuple` in a `Store` or `Del` context is marked (`^:store x`, `^:del
  x` on a symbol, `{:ctx :store}` on a list), and the converter checks each mark against its slot and drops it (S15).
  The literals' spellings are scanned in the source between each node's positions (§3; a replacement field is found
  by its node's start, so nested quotes need no lexer), and each `Constant`'s spelling is evaluated back to its
  value (`ast.literal_eval`), checked equal.
- **The printer's** `:lines` text has no positions, so a module's `# type: ignore` comments are placed one at the end
  of each of the first lines that end a statement or a block's header without a type comment: level 1 compares their
  tags, not their lines.
- **The round trip** `bin/repr-python roundtrip`: level 1 the forms (the original's against the print's dump,
  converted), level 2 `ast.dump`'s digest, level 3 the code's digest without positions: the code objects compiled
  from the text, their instructions with `NOP`s dropped, the superinstructions CPython fuses by line split again
  (`LOAD_FAST_LOAD_FAST` and the like), `LOAD_FAST_BORROW` as `LOAD_FAST`, a class body's `__firstlineno__` constant
  as a placeholder, `LOAD_SMALL_INT` as `LOAD_CONST`, each instruction's exception handler, a `frozenset` constant's
  elements sorted (the helper runs `-I`, which ignores `PYTHONHASHSEED`); the names, flags and nested code likewise.
  The survey's E7 ("code objects equal without positions") holds only after this normalization: on `Lib/` with
  `ast.unparse` as the printer, 395 of 1,869 files gave equal raw code without positions, 1,859 normalized.

PY3: the tokens and `:full`.

## 16. Sources

- CPython 3.14.10, the release tarball `Python-3.14.10.tar.xz` from `https://www.python.org/ftp/python/3.14.10/`
  (pinned in `test/python/corpora.edn`): `Parser/Python.asdl` (the nodes), `Grammar/python.gram`, `Grammar/Tokens`,
  `Lib/ast.py`, `Lib/tokenize.py`, `Doc/library/ast.rst`, `Doc/reference/lexical_analysis.rst`; read in place, not
  vendored.
- The oracle: the host's `/usr/bin/python3` 3.14.10 (`ast`, `tokenize`), probes of 2026-11-03 (in the task's scratch,
  not committed): the `ast.dump`s quoted in §3-§12.
- PEPs 263, 3131, 634 (`match`), 646, 654 (`except*`), 695, 696, 701 (f-strings in the grammar), 750 (t-strings),
  758 (`except A, B`).
- Arbace: doc/ocaml/SPEC.md (structure), doc/go/SPEC.md and doc/c/SPEC.md (operators' names), doc/python/PYTHON-SURVEY.md.
