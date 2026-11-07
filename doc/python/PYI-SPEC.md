# pyi: the scope analysis, the compiler and the IR

Status: PY5a (2026-11-03, the Mac VM, queue 179a), and PY5b0 (§7-§9: the runtime's interfaces and skeleton, queue
186), for the user's review. pyi is the interpreter of Python forms
([SPEC.md](SPEC.md)) the survey's §6 describes ([PYTHON-SURVEY.md](PYTHON-SURVEY.md); PYS7 (a): an IR of pyi's own on
heap frames; PYS8 (a): the runtime in `go/arbace/py`). This document fixes the parts PY5a built: the scope
analysis (§2), the IR the compiler makes (§3), the verifier that checks it (§4), and the checks against CPython
(§5); §6 states what the machine must give the IR, and §7-§9 (PY5b0) the runtime's interfaces, how it is worked
on, and which sub-milestone owns each of its files: the contract PY5b, PY5c, PY5d and PY5e build against in parallel.

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

## 7. The runtime: `go/arbace/py` (PY5b0)

The package `arbace/py` (Go forms in `go/arbace/py.ae` and `go/arbace/py/`), standard library only, interpreted by goi on
stage 0 (PY5a's DECISION (a): loaded from `ARBACE_PATH` as an interpreted package; listed in goi's table and compiled at
a later batched re-seed). Its Go text is the editing view (§8). PY5b0 gives the interfaces below and enough of each to run
a small module (`test/python/pyi/`); what it leaves out raises `SystemError: pyi: ... (PY5x's)` naming the owner.

### 7.1 Objects, types and slots (`object.go`, `type.go`)

- `type Object interface { Type() *Type }`: every Python value. Identity is Go's pointer identity, so every object is
  a pointer (the singletons `None`, `True`, `False`, `NotImplemented`, `Ellipsis` among them); the type is the object's
  header. A class's instances (PY5c) are a struct of their own carrying their `*Type`.
- `type Type struct`: `Name`, `Qualname`, `Module`, `Base`, `Bases`, `MRO`, `Dict` (the namespace), `Flags` (CPython's
  `tp_flags`: base type, heap type, the fast-subclass bits), `Slots`, `Meta` (the metaclass; nil is `type`).
  `t.IsSubtype(u)`, `t.Lookup(name)` (the MRO's dictionaries: `_PyType_Lookup`), `IsInstance(o, t)`.
- `type Slots struct`: the protocol functions (CPython's `tp_*` and the number, sequence and mapping structs):
  `Repr`, `Str`, `Hash` (and `Unhashable` for `__hash__ = None`), `Bool`, `Len`, `RichCompare(a, b, op)`, `GetAttr`,
  `SetAttr` (a nil value deletes), `Call(f, args, kwnames)`, `Iter`, `Next`, `GetItem`, `SetItem`, `Contains`,
  `Binary[op]`, `RBinary[op]` (the reflected method, called with the right operand first), `InPlace[op]`, `Unary[op]`,
  `Index`, `Int`, `Float`, `DescrGet`, `DescrSet`, `New`, `Init`. A nil slot is an unsupported protocol; the abstract
  functions fall back as CPython's do. A built-in type sets its slots in Go; a class's are filled from its special
  methods (PY5c, invalidated on assignment as CPython's version tags are). Adding a slot is a change to `type.go` and
  `inheritSlots` (its owner's: §9).
- **Calls**: `args []Object` holds the positional then the keyword arguments, `kwnames []*Str` the last
  `len(kwnames)` arguments' names (CPython's vectorcall convention; the IR's `:call [N KWNAMES]`).
- **Raising**: a runtime function returns `(Object, error)`; a non-nil error is an exception in flight, always a
  `*Raised{Exc *Exception}`. `Raise(t, msg)`, `Raisef(t, format, args...)`. The iterator protocol's end is
  `Next` returning `(nil, nil)`, not an exception. Go panics are bugs, never Python exceptions.
- **Numbers' and strings' representations** are private to their files; others use the constructors and accessors,
  which stay stable (PY5b, 2026-11-03):
  - `int`: arbitrary precision (an `int64`, a `*big.Int` beyond). `NewInt(int64)`, `NewIntBig(*big.Int)`,
    `(*Int).Int64() (int64, bool)` (false beyond 64 bits), `Big()`, `Sign()`, `AsInt(o)`, `ToInt(o)` (`int(x)`),
    `Index(o)`, `IndexInt(o)` (`__index__`), `ParseInt(s, base)`, `IntBinary(op, x, y)`, `IntPow(x, y, m)`,
    `MaxStrDigits` (`sys.set_int_max_str_digits`).
  - `str`: code points with lone surrogates, held as WTF-8. `NewStr(goString)`, `NewStrRunes([]rune)`, `String()`
    (the WTF-8 text), `Runes()`, `Len()`, `At(i)`, `AsStr(o)`; `Encode`, `Decode` (utf-8, ascii, latin-1 with the
    error handlers); `PercentFormat` (`str % x`).
  - `float`: `NewFloat`, `Value()`, `FloatRepr`, `ToFloat(o)`, `ParseFloat`, `FloatPow`, `FloatDivmod`; `complex`:
    `NewComplex`, `Value()`, `ComplexRepr`, `ComplexPow`; `bytes`: `NewBytes`, `Data()`, `AsBytes(o)`, `BytesRepr`;
    `set`/`frozenset`: `NewSet(frozen)`, `Add`, `Discard`, `Contains`, `Update`, `Items`, `Copy`, `AsSet`; `range`,
    `slice` (`NewSlice`, `Indices(n)`); `dict`: `Get`, `Set`, `Delete`, `Keys`, `Copy`, `Update`, `AsDict`; `tuple` and
    `list`: `Items`, `AsTuple`, `AsList`, `ToSlice(o)`, `ToTuple(o)`, `Iterate(o, f)`, `SortList`; `SetItem`, `Divmod`,
    `Abs`.
  - **The subclass convention**: an instance of a subclass of a built-in type is a struct of PY5c's that embeds the
    base's value struct (`Int`, `Str`, `Float`, `Complex`, `Bytes`, `Tuple`, `List`, `Dict`, `Set`, `Exception`); the
    embedded value's method `BaseInt()` (`BaseStr()`, ...) is promoted, and every slot of the base type finds the value
    through it. Each base type's `New` makes a subclass's instance through a hook PY5c sets (`newIntSubclass`,
    `newStrSubclass`, ..., `dictMissing` for `__missing__`).
  - **Hashing** is CPython's (E1, E9): numbers by 2**61 - 1, `str` and `bytes` by siphash13 under `HashKey` (zero:
    `PYTHONHASHSEED=0`, the references'), tuples by xxHash's rounds, frozensets by CPython's shuffle, `None`'s
    constant; `set`'s table is CPython's (probes, perturbation, dummies, growth), so a set's iteration order is
    CPython's for the same hashes.
- **Slots added by PY5b** (2026-11-03): `Abs`, `Divmod`, `RDivmod`, `Pow3` (pow with a modulus; `Pow3(a, b, m)`), and
  `Format` (`__format__`, for PY5c to fill).
- **Hooks for the other sub-milestones** (PY5b, 2026-11-03, their REQUESTs):
  - `BuiltinValueHook` (PY5c sets it to `BuiltinValue`): every core accessor (`asInt`, `AsStr`, `asException`, ...)
    tries it, so an instance of a class deriving from a built-in type is accepted where its base is. The core types'
    `New` called with a subclass makes the base value through `newIntSubclass`, `newStrSubclass`, ... (by default the
    plain base value, for PY5c to wrap or to replace).
  - `BoundMethodHook` (PY5c): a bound method of a Python function, called by `call` or `call-ex`, pushes the
    function's frame with the instance first (no nested run).
  - `newGeneratorHook` (PY5d): a call of a function whose code is `:generator` or `:coroutine` makes its generator,
    coroutine or async generator object of the bound frame instead of running it (CPython's `RETURN_GENERATOR`), in
    `call`, `call-ex` and `callFunction`.
  - `OnTypesReady(f)`: `f` runs once every type's slots are set and inherited, before the first interpreter.
    (`Dict` hashes a `*Str` key directly, not through `str`'s slot, so an `init` may fill a dictionary in any order.)
  - `ComputeMRO(t, bases)` (C3); a built-in type made with several `Bases` preset gets their C3 MRO (ExceptionGroup's).
  - `Raised.Reraise`: a bare `raise` and the IR's `reraise` add no traceback entry for their own frame.
  - `Exception.Ext`: a subtype's own data (an exception group's); `Exception.Field(name)` its fields (StopIteration's
    `value`, OSError's `errno`, SyntaxError's `lineno` ...).
  - `CodeIterableCoroutine` (`CO_ITERABLE_COROUTINE`, set at run time).
  - `for-iter` ends on a raised `StopIteration` as on `(nil, nil)`.
  - Traceback objects (`tb_frame`, `tb_lineno`, `tb_lasti`, `tb_next`) and frame objects (`f_code`, `f_lineno`,
    `f_lasti`, `f_globals`, `f_builtins`, `f_back`, `f_locals`: `FrameLocals`); functions' and code objects'
    attributes; `Code.Filename` (`co_filename`, `SetFilename`), which tracebacks print.
  - `FormatExceptionOnly(e)`: traceback.format_exception_only's text (a SyntaxError's location and caret, the notes);
    `PrintException` prints the chain (`__cause__`, `__context__`), each frame's source line.
  - `SortObjects(items, keys, reverse)` and `SortList(l, key, reverse)`: the one stable sort.
  - The compiler's modes (`arbace.python.compile/compile-source`: `:exec`, `:eval`, `:single`); in `:single` an
    expression statement's value goes to the new IR operation `print-expr` (`sys.displayhook`: PY5e's to implement);
    a compile-time SyntaxError carries `:lineno`, `:offset`, `:end-lineno`, `:end-offset` from the forms of mode
    `:full`.

### 7.2 The IR loader (`load.go`, `code.go`, `ops.go`)

`ReadIR(text)` reads a code object printed by `pr-str` (`arbace.python/ir-text`: the Clojure reader's syntax, the
subset the IR uses) into a `*Code`: the names, the constants as objects, the instructions decoded, the handler rows, the
lines, and `CellParams` (a cell that is a parameter, copied in on entry). An instruction is `Instr{Op, A, B}`:

| the IR's ARG | A | B |
|---|---|---|
| an index, a count, a jump target | it | 0 |
| `nil` | 0 | 0 |
| `true`/`false` (`:is`, `:contains`, `:call-ex`) | 1/0 | 0 |
| an operator symbol (`:binary`, `:inplace`, `:unary`, `:compare`) | `BinOp`, `UnOp`, `CompareOp` | 0 |
| `[N KWNAMES]` (`:call`) | N | the kwnames tuple's const index, or -1 |
| `[BEFORE AFTER]` (`:unpack-ex`), `[N ...]` (`:match-class`) | BEFORE | AFTER |
| `[NAME N]` (`:call-intrinsic`) | `Intrinsic` | N |
| `[CONST PARTS]` (`:make-function`) | CONST | `PartDefaults`, `PartKwDefaults`, `PartAnnotate`, `PartClosure` bits |
| `[CONVERSION SPEC?]` (`:format`, `:build-interpolation`) | the conversion's letter (`'r'`, `'s'`, `'a'`) or 0 | 1 when a spec |

`Op` has one constant per operation of `arbace.python.ir/ops`, in the order of their names (`opNames`), each with its
owner (`opOwners`, §9); `bin/pyi runtime check` requires the two lists equal. Constants PY5b0 does not load (doubles,
`{:imag}`, `{:bytes}`, `{:joined}`, ints beyond 64 bits) go through `loadConstHook`, PY5b's to replace.

### 7.3 The registry and `builtins` (`registry.go`, `builtins.go`)

Each file declares its types as package variables made by `NewBuiltinType(name, base, flags)` (no slots there: the
file's `init` sets them, so slot functions may name any type) and registers its built-in functions with
`RegisterBuiltin(name, fn)` (a name registered twice panics: one owner per name). `readyTypes` (run once by
`NewInterp`, after every `init`) completes each type: bases, MRO, and the nil slots inherited from its base
(`inheritSlots`, CPython's `inherit_slots`). An interpreter's `builtins` dictionary holds every registered public type
and function, `None`, `True`, `False`, `NotImplemented`, `Ellipsis`, in registration order (`hiddenTypes` names the
types CPython's `types` module names, not `builtins`).

### 7.4 Frames and threads (`frame.go`)

`Frame`: `Code`, `Func`, `Globals`, `Builtins`, `Locals` (a module's or class's namespace), `Fast` (nil is unbound),
`Cells` (the cells, then the free variables'), `Stack`, `PC` (the next instruction), `Lasti` (the running one: its line
is the traceback's), `Back`, `Suspended`. A frame is a heap object; `newFrame` binds a call's arguments (positional,
`*args`, keyword-only, `**kwargs`, defaults; CPython's `TypeError`s) and makes the cells. `Thread`: `Interp`, `Frame`
(the current one), `Handled` (the exception being handled: `push-exc-info`, `pop-except`, `sys.exception()`), the
depth against `Interp.RecursionLimit` (1,000). One thread runs at a time (`currentThread()`, the GIL's holder:
PY6). `Interp`: `Builtins`, `Modules`, `Stdout`, `Stderr`, `RecursionLimit`, `Main`.

### 7.5 The dispatch loop (`eval.go`)

`(*Thread).run(f)` runs `f` and every frame it calls until `f` returns; `step(f)` runs `f`'s instructions until it
returns a value, raises, or calls a Python function, which it signals as `*pushFrame`: `run` then continues with the
callee's frame, so Python's calls never deepen Go's stack (PYS7 (a)); a return pops to `Back` and pushes the value. A
call from Go (a slot calling a Python function: `Function`'s `Call`) is a nested `run` of an entry frame. An exception
at `Lasti` adds a `Traceback` entry (outermost first, `Next` inward, as CPython links them) and looks up the covering
handler (§3.3): the stack popped to its depth, `lasti` pushed when the row says so, the exception pushed, `PC` its
target; with no handler the frame is left and the caller's call instruction raises it, up to the entry frame.

`step`'s switch holds the operations the loop owns (§9). Any other operation is looked up in `opImpls`, which the file
that owns it fills from its `init` by `RegisterOp(op, impl)`: `impl(t, f, in)` pops and pushes `f`'s stack and returns
`(nil, nil)` to go on, `(nil, err)` to raise, `(nil, *pushFrame)` to run a frame, or `(v, nil)` to make `f` return `v`
(a yield sets `f.Suspended` first; PY5d's generators keep the frame and resume it by `run`). So PY5c, PY5d and PY5e add
operations without editing `eval.go`.

### 7.6 Entry points (`interp.go`)

`NewInterp(stdout, stderr)`, `(*Interp).ExecModule(code, name)`, `ExecModuleFile(code, name, file)`,
`PrintException(err)` (CPython's default hook: the traceback's frames and lines, then `Type: text`; PY5b0 without
chained exceptions, source lines or carets), and `RunModule(ir, file)`, the process's run (exit status 0, 1 for an
uncaught exception, `SystemExit`'s code), which `bin/pyi run` calls through goi.

### 7.7 What PY5b0 runs

The operations of `step`'s switch and `load-attr`; the types `object`, `type`, `NoneType`, `bool`, `int` (64 bits),
`str` (concatenation, comparison, `in`), `tuple` and `list` (repr, length, indexing, iteration by `__getitem__`), `dict`
(insertion order, hashing and equality), functions with every kind of parameter, closures, cells, modules, 20
exception types with `raise`, `raise from`, `try`/`except`/`finally` and tracebacks; the built-ins `print` (`sep`,
`end`, `flush`), `repr`, `len`. `test/python/pyi/` (4 programs) runs equal to the oracle (stdout, status, stderr's
last line). Of the synthetic corpus and the samples (151), 7 run equal to their references (`ref-cpython314.edn`); the
others stop at a constant or an operation of a later sub-milestone: floats and bytes (PY5b, 57), classes (`load-build-class`,
PY5c, 42), imports (PY5e, 11), generators (PY5d, 4), and builtins not registered yet.

### 7.8 The data model (PY5c)

PY5c's files (`class.go`, `descr.go`, `attr.go`, `format.go`, `annotate.go`, `typing.go`, and `abstract.go`) give the
other files these interfaces:

- **Classes.** A class statement's type is a `*Type` with `FlagHeapType`, its `Meta` the metaclass (nil for `type`),
  its MRO by C3. Its instances are `*Instance`. A class deriving from a built-in type other than `object`,
  `BaseException` and `type` keeps the built-in's value in its instance, and its inherited slots work on that value.
  `BuiltinValue(o)` is that value (`o` itself for any other object): a core type's function taking its subclasses
  (`int(MyInt(5))`, `asInt`) should call it. An exception class's instances are PY5b's `*Exception`, its `T` the class.
- **Slots of a class** are filled by `fixupSlots` from the special methods of the classes in its MRO, over the slots
  of its first built-in type in the MRO. They are refilled for the class and its subclasses when a special attribute
  is assigned or deleted on the class (`type.__setattr__`).
- **Lookup.** `lookupAttr(t, name)` is `_PyType_Lookup` over the MRO. For each type it checks the dictionary, then the
  type's `GetSet`s (`AddGetSet(t, name, get, set)`: CPython's `tp_getset` and `tp_members`), then, for a built-in
  type, its slots seen as methods (slot wrappers: `object.__init__`, `int.__add__`, `type.__call__` ...). The other
  lookups build on it: `GenericGetAttr` and `GenericSetAttr` (object's), `typeGetAttr` and `typeSetAttr` (type's),
  `GetAttr`, `SetAttr`, `DelAttr`, `GetAttrOptional`, `LookupSpecial(o, name)` (a special method bound to `o`, or
  nil), and `CallSpecial(o, name, args...)` (found, result: for the built-ins that have no slot, such as `__abs__`,
  `__divmod__`, `__round__`, `__reversed__`, `__length_hint__`, `__fspath__`).
- **Built-in methods.** `AddMethod(t, name, fn)` and `AddToDict(t, name, v)` put entries in a built-in type's dictionary
  once the runtime is ready. An `init` function may run before `str.go`'s, and a dictionary hashes its keys by
  str's slot. A `*Builtin` found on a built-in type is bound to the instance, or to its value for a subclass's
  instance.
- **Functions.** `IsInstanceOf(o, cls)` and `IsSubclass(c, cls)` honour `__instancecheck__` and `__subclasscheck__`
  (`isinstance` and `issubclass` should call them). `Format(o, spec)` is `format()`; `RegisterFormatter(t, f)` sets
  a built-in type's `__format__` (a float's, once floats exist). `Ascii(o)` is `ascii()`.
- **Builtins PY5c registers:** `__build_class__` and `format`. The types it registers: `property`, `classmethod`,
  `staticmethod` and `super`, and these hidden types: `method`, `getset_descriptor`, `member_descriptor`,
  `wrapper_descriptor`, `mappingproxy`, `TypeVar`, `TypeVarTuple`, `ParamSpec`, `TypeAliasType`, `GenericAlias`,
  and `string.templatelib`'s `Template` and `Interpolation`. `TemplateLib()` returns the names that module needs.
- **Slots of `object` and `type`.** PY5c sets `ObjectType.Slots.New` and `Init` (object's `__new__` and `__init__`, with
  CPython's excess-argument rules) and `TypeType.Slots.New`, `Init`, `GetAttr` and `SetAttr`.

## 8. Working on the runtime

The forms are in mode `:full` (repr/go's lossless mode: comments, layout and spellings kept), read for linux/amd64, so
the Go text prints back byte for byte and is the editing view:

```
bin/pyi runtime print       # go/arbace/py as a Go module in .tmp/repr-python/pyi/runtime (go.mod "module arbace", py/)
...edit .tmp/repr-python/pyi/runtime/py/*.go...
bin/pyi runtime vet         # the Go being edited (printed first when absent): gofmt clean, go vet, go test by TamaGo's go
bin/pyi runtime read        # the edited Go read back into go/arbace/py (repr/go's native reader, --full --tests)
bin/pyi runtime check       # printed and read again equal to the committed forms; the operations equal the IR's;
                            # test/python/pyi/ run by pyi equal to the oracle
bin/pyi run FILE.py         # a program run by pyi on goi
```

Rules: `gofmt` clean, `go vet` clean; `go test ./py` passes (TamaGo's go builds and tests the printed module natively,
seconds, before goi runs it); no package outside the standard library; no `unsafe`, `reflect` or cgo; `print`, edit,
`vet`, `read`; never hand-edit the forms. `print` overwrites the edited copy.

## 9. Owners

One owner per shared file; another sub-milestone that needs a change there asks its owner (or adds a file of its
own and registers through §7.3 and §7.5's hooks). New files take the owner's prefix in the table.

| file(s) | owner | what |
|---|---|---|
| `object.go`, `type.go` (the `Slots` struct, `inheritSlots` in `registry.go`) | PY5b | the object header, the type layout, the slot table |
| `registry.go` | PY5b | the type registry, `readyTypes` |
| `code.go`, `load.go`, `ops.go` | PY5b | the IR's Go form, the loader, the operation table (owners per operation) |
| `frame.go`, `eval.go` | PY5b | frames, threads, binding, the loop, unwinding |
| `interp.go` | PY5b | the interpreter, entry points, the default exception hook |
| `core.go`, `int.go`, `str.go`, `containers.go`, `func.go`, `exc.go`; new `float.go`, `complex.go`, `bytes.go`, `set.go`, `range.go`, `slice.go` | PY5b | the core types |
| `abstract.go`; new `class.go`, `descr.go`, `attr.go`, `format.go`, `annotate.go`, `typing.go` | PY5c | the data model: classes, descriptors, attribute lookup, the operators' rules, formatting, f- and t-strings, annotations, the typing intrinsics |
| new `gen.go`, `coro.go`, `with.go`, `excgroup.go`, `match.go` | PY5d | generators, coroutines, `with`, `except*`, `match` |
| `builtins.go`; new `builtins_*.go`, `sys.go`, `repl.go`, `import.go` | PY5e | the builtins module, `sys`, the REPL, single-file imports |
| `py_test.go` (PY5b); new `*_test.go` by their file's owner | each | Go tests |
| `arbace/python.ae`, `arbace/python/compile.ae`, `arbace/python/ir.ae`, `bin/pyi`, `test/python/pyi/` (each adds programs with its prefix) | PY5b | the compiler's fixes the runs find, the verbs |

The operations' owners are `opOwners` (`ops.go`): PY5b 66 (the stack, names, operators, containers, calls, jumps,
iteration, returns, exceptions), PY5c 11 (attributes, `load-build-class`, formatting and templates, the intrinsics,
`setup-annotations`, `exit-init-check`), PY5d 18 (yield, await, async iteration and `with`, `except*`'s, `match`'s), PY5e 3
(imports).

## 10. The AOT (PY8)

`arbace.python.aot` (PYS13 (a): straight to Go forms, as Scheme's `arbace.scheme.aot`) translates the IR of §3 into Go
forms over `go/arbace/py`, a compiled interpreter (the survey's §7):

- **A code object is a Go function** `NativeFunc` (`func(t *Thread, f *Frame) (Object, error)`, `code.go`) over the
  machine's own heap frame: the frame's stack, locals, cells, `PC` and `Lasti` are the loop's, so the loop (`Thread.run`)
  runs it in place of `step` (`Code.Native`, set by the loader's `irLoadedHook`). Its body: an entry `switch f.PC` with
  a case for every instruction (`goto Ln`), then each instruction under its label: `f.Lasti = n` and Go statements
  calling `aot.go`'s helpers with the argument as a constant (`AotLoadFast(f, 3)`, `AotBinary(f, BinAdd)`, ...), a jump
  a `goto`, a conditional jump or `for-iter` a test and a `goto`. An error returns at once and the loop unwinds it by
  the code's exception table, setting `PC` to the handler and running the function again, whose switch enters at the
  handler; a call of a Python function (`AotCall`, with `PC` set to the next instruction) returns the loop's
  `*pushFrame`, so Python calls never deepen Go's stack (RecursionError where CPython raises it) and the caller resumes
  by its switch. A generator's or coroutine's frame resumes the same way: a state machine on the resume point over the
  heap frame, `yield-value`, `yield-from`, `throw` and `close` the loop's own (PY5d's operations).
- **The operations the helpers do not spell out** (the rarer ones of `step`'s switch, and every operation another file
  registers with `RegisterOp`: attributes, `with`, `match`, imports, formatting, `except*`, yields ...) run by
  `AotExec(t, f, n)`: the loop's implementation of instruction `n`, already decoded by the loader, with `PC` set before
  and a `goto dispatch` after when the operation moved it.
- **A program** is its main module and the modules its module-level code imports (the code of the module and its class
  bodies, not its functions: `import-name` after its level and from-list constants, each dotted prefix and from-list
  submodule), found on the run's `sys.path` (the program's directory, the pinned tarball's `Lib`) as `import.go` finds
  them, compiled by `arbace.python.run/compile-source` (the compiler pyi's imports use) and each translated into a package
  `arbace/pyaot/mHASH` (HASH of its IR text; a module compiled once is the same package for every program, printed once
  and kept by `bin/pyi`): the IR text, its functions in the loader's pre-order (the code object, then its nested code
  objects in constant order), `Register` (`AotRegister` by the source's `AotKey`, the SHA-256 of the mode and the
  source). The main package calls each `Register` and `AotMain`: `sys.argv` (the process's arguments, else the file),
  `sys.path`, the compiler (`SetCompiler`: a source compiled ahead by its key, else the host's) and `RunModule`.
- **Source compiled at run time** (`exec`, `eval`, `compile`, an import not found statically) goes to the host's
  compiler: the command `PYI_AOT_COMPILER` names, started once (`arbace.python.aot compile-server` on stage 0, requests
  and answers by length on its stdin and stdout), and runs on the loop. `PYI_AOT_STATS` names a file for the counts (code
  objects compiled and on the loop, the host's compiles), `PYI_AOT_LOOP` runs every code object on the loop (the check's
  interpreted reference on the same runtime, the speed's baseline), `PYI_CPUPROFILE` writes Go's CPU profile.
- **The checks**: `bin/pyi aot FILE [ARGS]` (one program compiled and run), `bin/pyi check --aot [--record] [--no-pyi]
  [--only ID]` (§11 of the survey's PY8: the synthetic corpus and the samples against 177's references, the loop and pyi;
  classes `test/python/divergences-aot.edn`, the rule `:as-pyi`), `bin/pyi bench [--pyi] [--record] [BM...]`
  (pyperformance 1.14.0's pure benchmarks, `test/python/bench/`). The Go builds use TamaGo's go alone, in
  `.tmp/repr-python/pyi/aot/mod` (the runtime printed by `runtime print`, the packages of `arbace/pyaot`), never
  `bin/arbace-go`; out of `gate_key` as the rest of the crop (PYS18 (a)).

Owners: `arbace/python/aot.ae`, `go/arbace/py/aot.go`, `test/python/bench/`, `test/python/divergences-aot.edn` and
`bin/pyi`'s `aot`, `check --aot` and `bench` are PY8's; its hooks in PY5b's files are `Code.Native` and `NativeFunc`
(`code.go`), `step`'s first statement (`eval.go`) and `irLoadedHook` (`load.go`).
