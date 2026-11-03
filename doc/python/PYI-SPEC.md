# pyi: the scope analysis, the compiler and the IR

Status: PY5a (2026-11-03, the Mac VM, queue 179a), for the user's review. pyi is the interpreter of Python forms
([SPEC.md](SPEC.md)) the survey's §6 describes ([PYTHON-SURVEY.md](PYTHON-SURVEY.md); PYS7 (a): an IR of pyi's own on
heap frames; PYS8 (a): the runtime in `go/arbace/py`). This document fixes the parts PY5a built: the scope
analysis (§2), the IR the compiler makes (§3), the verifier that checks it (§4), and the checks against CPython
(§5). The frame machine that runs the IR (PY5b) and the object model are later milestones; §6 states what they
must give the IR.

Everything here is evaluated from the checkout on the tools' host, outside `gate_key` and the gate (PYS18 (a)):
`arbace/python.ae` and `arbace/python/` are excluded like the other crops' namespaces (`bin/lib/version.bash`'s
`arbace_embedded_sources`, `arbace/j2g/embed.ae`, `test/gate/audit.ae`, `test/gate/steps.edn`), and so is
`go/arbace/py`, the runtime's place (`bin/arbace-go`'s `gate_key`; `test/gate/gate-key-crops-test.bash` checks both).

| namespace | what | source |
|---|---|---|
| `arbace.python.scope` | the symbol table | a port of CPython 3.14.10's `Python/symtable.c` and the `__future__` part of `Python/future.c` |
| `arbace.python.compile` | forms → code objects | after `Python/codegen.c` and `Python/compile.c`, emitting the IR of §3 instead of bytecode |
| `arbace.python.ir` | the IR's operations, stack effects and verifier | Arbace's own |
| `arbace.python` | `bin/pyi`'s verbs `scope-check` and `compile-check` | Arbace's own |

## 1. Words

- **a block**: a symbol-table entry, as CPython's `PySTEntryObject`: a module, class, function (a `def`, a lambda, a
  generator expression or a comprehension not inlined), annotation scope (PEP 649), type-alias, type-parameters or
  type-variable scope (PEP 695).
- **a code object**: the IR of one block (§3.1).
- **a unit**: the compiler's state while it emits one code object (CPython's `compiler_unit`).
- **an fblock**: a frame block on the unit's stack (a loop, `try`, `with`, an `except` handler), for `break`,
  `continue` and `return` to unwind (CPython's `fblockinfo`).

## 2. The scope analysis

`(arbace.python.scope/analyze MODULE-FORM)` builds the table in CPython's two passes: the first visits the forms,
entering a block per scope and recording each name's flags (`DEF_GLOBAL` 1, `DEF_LOCAL` 2, `DEF_PARAM` 4,
`DEF_NONLOCAL` 8, `USE` 16, `DEF_FREE_CLASS` 64, `DEF_IMPORT` 128, `DEF_ANNOT` 256, `DEF_COMP_ITER` 512,
`DEF_TYPE_PARAM` 1024, `DEF_COMP_CELL` 2048); the second (`analyze_block`) resolves each name's scope, stored in the
flags' bits 12 and up as CPython stores it (`LOCAL` 1, `GLOBAL_EXPLICIT` 2, `GLOBAL_IMPLICIT` 3, `FREE` 4,
`CELL` 5). Ported as written: the private-name mangling (`_Py_Mangle`, with a class's type parameters exempt), PEP
709's inlined comprehensions (their names merged into the enclosing block, `DEF_COMP_CELL` for a cell among them),
the implicit cells of a class (`__class__`, `__classdict__`, `__conditional_annotations__`: `needs_class_closure`,
`needs_classdict`, `has_conditional_annotations`), annotation blocks (`can_see_class_scope`, one per module or class,
one per function's signature), the type-parameter blocks with `.type_params` and `.generic_base`, and every error of
`symtable.c` with its message.

It returns `{:top BLOCK :keys MAP :keys-bound MAP :keys-default MAP :features SET}`: the module's block (an atom of
`{:type :name :symbols {NAME FLAGS} :varnames [...] :children [...] ...}`), identity maps from the forms that open a
block to their block (CPython keys its entries by the AST node's address: a function statement, a function's
`arguments` form for its annotation block, a type-parameter vector, a class statement, a `type` statement, a lambda
or comprehension expression, a type parameter for its bound's and its default's lazy blocks), and the `__future__`
features. `(table MODULE-FORM)` prints the tree as `(block TYPE "NAME" {"SYM" FLAGS ...} [CHILD ...])`, the form the
helper writes from CPython's `_symtable` (`tools/pydump/pydump.py scopes`), so the two compare with `=`.

A `SyntaxError` is an `ex-info` `{:class SyntaxError :msg MESSAGE}`; the message is CPython's.

## 3. The IR

### 3.1 Code objects

A code object is a map (`arbace.python.ir`'s docstring):

```clojure
{:name "f" :qualname "C.f" :kind :function   ; :module :class :function :lambda :comprehension :annotation
                                             ; :type-params :type-alias :type-variable
 :flags #{:generator :coroutine :varargs :varkw :optimized :nested :has-docstring}
 :argcount 2 :posonly 0 :kwonly 1            ; as co_argcount, co_posonlyargcount, co_kwonlyargcount
 :locals ["self" "x" "k" "tmp"]              ; fast locals, the parameters first, in order (*args, **kw after kw-only)
 :cells ["x"] :frees ["__class__"]           ; cell variables (sorted), then the free ones (sorted)
 :names ["print" "append"]                   ; global, name and attribute names, by first use
 :consts [nil 1 "s" {:code CODE} ...]         ; constants, nested code objects as {:code CODE}
 :code [[:load-fast 0] [:return nil] ...]    ; the instructions
 :handlers [[START END TARGET DEPTH LASTI?]] ; the exception table
 :lines [LINE-OR-NIL ...]                    ; each instruction's statement line
 :docstring "..."}
```

A parameter that is also a cell keeps its local slot; the machine copies it into its cell on entry (CPython's
`MAKE_CELL`). Constants are Arbace values: `nil` for `None`, `true`/`false`, integers (`Long` or `BigInteger`),
strings, doubles, `{:imag DOUBLE}`, `{:bytes [..]}`, `{:tuple [..]}`, `:ellipsis`, `{:code ..}`, and a string with a `\N{NAME}` escape as `{:joined [part {:named "NAME"} ...]}` (the name's lookup in
Unicode's data is the runtime's, §6).

### 3.2 Instructions

An instruction is `[OP ARG]`. A jump's `ARG` is an instruction index. `arbace.python.ir/ops` lists every operation
with its stack effect and whether it jumps or ends the block; the list is the reference, grouped here:

- **stack**: `:nop`, `:const` (an index into `:consts`), `:pop`, `:copy N`, `:swap N`.
- **names**, by the name's scope in its block (CPython's `compiler_nameop`):

  | scope | in an optimized block (function-like) | in a module or class |
  |---|---|---|
  | `LOCAL` | `:load-fast` `:store-fast` `:delete-fast` (a `:locals` index) | `:load-name` ... (a `:names` index) |
  | `CELL`, `FREE` | `:load-deref` `:store-deref` `:delete-deref` (cells then frees) | the same; a class's free name loads with `:load-locals` + `:load-dict-or-deref` |
  | `GLOBAL_EXPLICIT` | `:load-global` ... | `:load-global` ... |
  | `GLOBAL_IMPLICIT` | `:load-global` ...; in a block that sees its class, `__classdict__` then `:load-dict-or-global` | `:load-name` ... |

  An inlined comprehension's own names are hidden fast locals of the enclosing unit: `:load-fast-and-clear` saves
  the outer binding (or the marker `:unbound`) before the loop, `:store-fast` restores it after (`:store-fast` of
  `:unbound` unbinds); an own name that is a cell in the comprehension is saved with `:load-cell-and-clear` (the
  cell itself, a fresh one put in its place) and restored with `:store-cell`. `:load-closure` pushes a cell for a
  nested function's closure tuple.
- **operators**: `:binary OP`, `:inplace OP` (`+ - * / floor-div % ** << >> & | ^ @`), `:unary OP` (`+ - bit-not`),
  `:not`, `:to-bool`, `:compare OP` (`== != < <= > >=`), `:is NEGATED?`, `:contains NEGATED?`.
- **containers**: `:build-tuple N`, `:build-list N`, `:build-set N`, `:build-map N` (2N items), `:build-slice 2|3`,
  `:list-append N`, `:set-add N`, `:map-add N` (into the container N items down), `:list-extend 1`,
  `:set-update 1`, `:dict-update 1`, `:dict-merge 1` (a duplicate keyword is a `TypeError`), `:list-to-tuple`,
  `:unpack-sequence N`, `:unpack-ex [BEFORE AFTER]` (pushed so the first item is on top).
- **attributes and items**: `:load-attr`, `:store-attr`, `:delete-attr` (a `:names` index), `:subscr`,
  `:store-subscr`, `:delete-subscr`.
- **calls**: `:call [N KWNAMES]` (the callable under N arguments, the last `count KWNAMES` of them keyword
  arguments named by the constant tuple `KWNAMES`, or `nil`), `:call-ex KW?` (callable, a tuple, a dict when `KW?`),
  `:call-intrinsic [NAME N]` (§3.4), `:make-function [CONST PARTS]` (PARTS in stack order among `:defaults`,
  `:kwdefaults`, `:annotate`, `:closure`; the type parameters are an intrinsic), `:load-build-class`,
  `:load-locals`.
- **control**: `:jump`, `:jump-if-false`, `:jump-if-true`, `:jump-if-none`, `:jump-if-not-none` (each pops),
  `:return`, `:raise 0|1|2`, `:reraise`.
- **iteration**: `:get-iter`, `:for-iter` (pushes the next item, or at the end pops the iterator and jumps),
  `:get-aiter`, `:get-anext`, `:end-async-for`, `:get-awaitable`, `:get-yield-from-iter`, `:yield-value`,
  `:yield-from` (`[iterator first-value]` → the delegation's result: CPython's `SEND`/`YIELD_VALUE`/`END_SEND`
  loop as one operation, its suspension points the machine's).
- **exceptions**: `:push-exc-info`, `:pop-except`, `:check-exc-match`, `:check-eg-match`, `:prep-reraise-star`.
- **`with`**: `:with-enter`, `:async-with-enter` (manager → exit, the result of enter), `:with-except` (exit, lasti,
  previous exception, exception → the same and exit's result).
- **imports**: `:import-name`, `:import-from`, `:import-star`; `:setup-annotations` (`__future__` annotations).
- **strings**: `:format [CONVERSION SPEC?]`, `:build-string N`, `:build-interpolation [CONVERSION SPEC?]` (value
  and its source text: a t-string's `Interpolation`), `:build-template N`.
- **`match`**: `:match-class N`, `:match-mapping`, `:match-sequence`, `:get-len`, `:match-keys`,
  `:copy-dict-without-keys`.

### 3.3 Exception handlers

Each instruction is covered by at most one handler, the innermost active when it was emitted: the compiler keeps a
handler stack (CPython's `SETUP_FINALLY`/`SETUP_CLEANUP`/`SETUP_WITH` ... `POP_BLOCK` pseudo-instructions, resolved as
CPython's `flowgraph.c` resolves them), each instruction records the top, and runs of one handler become the table's
rows `[START END TARGET DEPTH LASTI?]` (END exclusive). On an exception at an instruction the machine pops the stack
to DEPTH, pushes the instruction's index when LASTI? and then the exception, and jumps to TARGET; with no row the
frame is left. Unwinding by `break`, `continue` and `return` runs each fblock's cleanup under the handlers outside
it (each fblock records the handler stack of its entry). The shapes, from `codegen.c`:

| statement | handler entry | the cleanup |
|---|---|---|
| `try`/`except` | `[exc]` at the try's depth | `push-exc-info`, each clause's `check-exc-match`, the body under a cleanup handler (`[prev lasti exc]`: `swap 2, pop, swap 2, pop-except, reraise`) |
| `try`/`finally` | `[lasti exc]` | `push-exc-info`, the finally body, `reraise`; the normal path runs the body inline |
| `with` | `[exit lasti exc]` | `push-exc-info`, `with-except`, `to-bool`, re-raise unless true |
| `except*` | `[exc]` | §3.5 |
| `async for` | `[iter exc]` | `end-async-for` |

### 3.4 Intrinsics

`:call-intrinsic [NAME N]` calls a runtime helper on the top N items: `:not-implemented-error` (an annotate
function's format check), `:assertion-error`, `:typevar`, `:typevar-with-constraints`, `:typevartuple`,
`:paramspec`, `:typealias`, `:subscript-generic`, `:set-function-type-params`.

### 3.5 Statements as CPython compiles them

The compiler follows `codegen.c` statement by statement: its evaluation order (a call's callable before its
arguments, a dict display's key before its value, an augmented subscript's container and key once), its implicit
names and cells. The parts worth naming:

- **classes**: the body stores `__module__`, `__qualname__`, `__firstlineno__` (when lines are known), `__type_params__`,
  the `__classdict__` cell, the `__conditional_annotations__` cell, `__doc__`, the body, `__annotate_func__` (a class's
  annotate function, which the class creation exposes as `__annotate__`, CPython's `type_new`), `__static_attributes__`,
  `__classdictcell__` and `__classcell__`; the class is built by `load-build-class` called on the body function, the
  name and the bases (`call` with the two extra arguments, or `call-ex` with them first in the tuple).
- **annotations (PEP 649)**: a module's or class's simple annotations become one `__annotate__` function (the
  `format > 2` check, then a dict); a conditional one (a module's always, a class's in an `if`, loop, `try`, `with` or
  `match`) is recorded by index into `__conditional_annotations__` and included only when its index is in the set.
  A module's annotate function is stored at the module's start (CPython's `ANNOTATIONS_PLACEHOLDER`: a module
  executed in part has one). A function's annotations are its own annotation block's function (`:annotate`). Under
  `from __future__ import annotations` a module or class with annotations runs `setup-annotations` and stores each
  annotation's text (the printer's) in `__annotations__`; a function's annotate function returns the texts.
- **comprehensions**: inlined (PEP 709) when the table inlined them, else a nested function called on the outermost
  iterator; generator expressions always nested.
- **type parameters (PEP 695)**: a generic function, class or alias is built inside a type-parameters function; each
  bound, constraint tuple and default is a lazy type-variable function.
- **`match`**: CPython's pattern compilation (a capture or wildcard allowed only where CPython allows one, the
  errors of duplicate names, different names in alternatives, duplicate keys and repeated attributes), with each
  failure jump popping what it left (a fail ladder).
- **`except*`** (CPython's `codegen_try_star_except`): `[prev orig]`, a list `res` and the group still unhandled;
  each clause splits it (`check-eg-match` → rest, match), binds or pops the match and runs its body under a handler
  that appends what the body raises to `res`; after the last clause the rest is appended too and
  `prep-reraise-star` (the original group and `res`) gives what to re-raise, or `None`.

## 4. The verifier

`(arbace.python.ir/verify CODE)` is `nil` when the code object holds, else a message naming the code and the
instruction. It checks every op is known; every jump and handler target is inside the code; every local, cell,
free, name and constant index is in range; and, by a worklist over the control flow (each instruction's successors
and the handler covering it), that the stack's depth at each reachable instruction is the same on every path into
it, never negative, never below what an instruction pops, that a handler's DEPTH is not above the depth of an
instruction it covers, that control does not fall off the end, and that every `return` leaves exactly its value on
the stack (each block it leaves unwound). It verifies the nested code objects too.

## 5. The checks

`bin/pyi scope-check [CORPUS...]` (the default every read corpus: samples, synthetic, errors, stdlib,
site-packages, lib-test): for each file the helper writes its tree (`pydump.py tree`) and CPython's symbol table
(`pydump.py scopes`); `arbace.python.scope/table` must equal it, or a file CPython rejects must be rejected with
CPython's message. `bin/pyi compile-check [CORPUS...]`: each file compiled and its code objects verified (§4); a file
CPython's `compile` rejects (the helper's TSV, `compile-error: MESSAGE`) must be rejected with the same message, and
a file CPython compiles must compile. Results by file against `test/python/scope-known.edn` and
`test/python/compile-known.edn` (the round trip's known-file format; `--record` writes them, a file of no class
exits 3). The corpus `errors` (`test/python/errors/`) holds one program per message of `symtable.c` and, as
`compile-*.py`, one per message of `codegen.c` that the parser lets through.

Not checked yet: the IR against CPython's bytecode (the two differ by design: §6), and the IR's behaviour, which
PY5b's machine checks by running the synthetic corpus and `Lib/test` against PY4's references.

## 6. The machine's side (PY5b)

The IR is CPython-like in shape and Arbace's in detail; what the machine (`go/arbace/py`) must give it:

- frames on the heap holding the locals, the cells, the stack and the instruction index, so a generator or coroutine
  suspends at `yield-value` and `yield-from` by returning from the loop with its frame kept (PYS7 (a));
- the handler table's semantics of §3.3, `lasti` for `with` and `finally` re-raises, and the exception state
  `push-exc-info` and `pop-except` save and restore (`sys.exception()`);
- the marker `:unbound` of the inlined comprehensions, and the cells' copy of the parameters on entry;
- the intrinsics of §3.4 and the operations' semantics (the object model of the survey's §6.4);
- the lines: `:lines` holds statement lines from forms read in mode `:full` (`:pos`); the forms of mode `:lines`
  carry none, so tracebacks and `__firstlineno__` need the forms of `:full` (or the `:lines` mode's line metadata,
  PY5b's choice).

Where the IR differs from CPython 3.14's bytecode on purpose: no specialization, caches or superinstructions; no
`RESUME`, `PUSH_NULL` or `NOT_TAKEN`; one `yield-from` for the `SEND` loop; constant folding not done (CPython's AST
optimizer folds constants, which is observable only through `co_consts` and the `dis` module: classed when it shows).
