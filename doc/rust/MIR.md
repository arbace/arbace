# MIR forms (spec)

Status: RF2, 2026-11-05 (doc/rust/FRONTEND-SURVEY.md, "As built: RF1-RF3 (trial)"). MIR forms are rustc's MIR as data: what the
front end (RF4-RF11) will produce and what the back half (RF3) consumes. Today they come from the helper (`tools/rsdump/mirdata.rs`,
`rsdump rustc --mir-data FILE -- RUSTC-ARGS...`, rustc 1.99.1) and are read by `arbace.rust.front.mir`. The text of MIR
(`-Zunpretty=mir`, `rustc_middle/src/mir/pretty.rs`) is the judge: forms printed back by `arbace.rust.front.mir/print-dump` equal the
helper's `--mir` text (`bin/rust-front mir-check`).

## 1. A dump

A file of forms, one per body, in the order `write_mir_pretty` prints them (`tcx.mir_keys`, with the same choice of body per item):

- `(mir/body {...})`: `optimized_mir` (runtime-optimized MIR) of an item, then its promoteds (`promoted_mir`); a `const fn` also its
  `mir_for_ctfe` (`:ctfe true`, `:phase "ctfe"`); a `#[rustc_comptime]` function only the CTFE body; any other item `instance_mir`.
- `(mir/trivial-const {:def :ty :text})`: a constant rustc prints as one line.

Other phases (`built`, `promoted`, `drops-elaborated`) are not dumped as data: their queries are stolen by later passes in a
`rustc_driver` run (`Steal`); `-Zdump-mir` gives their text, and RF10 compares ours with it.

## 2. A body's map

| key | value |
|---|---|
| `:def` | `def_path_str` as `write_mir_pretty` prints a path (no crate prefix for the local crate), `:name` with impls as `<impl at FILE:L:C: L:C>`, `:def-id` rustc's `DefId` Debug, `:instance` the `InstanceKind` Debug |
| `:kind` | `:fn`, `:const`, `:static`, `:static-mut`, `:anon-const`, `:global-asm`, `:other` (the prefix `write_mir_sig` prints); closures are `:fn` |
| `:phase`, `:ctfe`, `:promoted` | as above; `:promoted N` for `promoted[N]` |
| `:args`, `:ret`, `:yields` | the argument count (locals `1..args`), the return type, a coroutine's yield type |
| `:locals` | a vector indexed by local: `{:mut :scope :ty [:user-ty]}` |
| `:scopes` | a vector indexed by scope: `[PARENT INLINED-TEXT]` (nil parent for scope 0) |
| `:debug` | `{:name :scope :value [:composite {:ty :proj}]}` per `var_debug_info`, in order; the value is a place or a constant |
| `:blocks` | a vector indexed by block: `{:cleanup :stmts :after-dbg :term}` |
| `:allocs` | the text `write_allocations` prints after the body (allocation bytes; opaque in this version) |

## 3. The shapes

Types: `(ty "TEXT" STRUCT)`. TEXT is `{}`'s rendering (what the printer prints), STRUCT its structure: `bool`, `char`, `str`,
`never`, `(int "i32")`, `(uint "u8")`, `(float "f64")`, `(tuple T...)`, `(array T "N")`, `(slice T)`, `(ref T mut?)`, `(ptr T mut?)`,
`(adt "PATH" "DEFID" [ARG...])`, `(fndef "PATH" [ARG...])`, `(closure "PATH" [ARG...])`, `(param "T")`, `(fnptr)`, `(dyn)`, `(alias)`,
`(foreign)`, `(other KIND)`; generic arguments: types, `(lt)`, `(cst "TEXT")`.

Places: `(place LOCAL PROJ...)`, PROJ one of `(deref)`, `(field N T)`, `(index LOCAL)`, `(cindex OFFSET MIN FROM-END)`,
`(subslice FROM TO FROM-END)`, `(downcast NAME-OR-NIL VARIANT)`, `(opaque T)`, `(unwrap-binder T)`.

Operands: `(copy PLACE)`, `(move PLACE)`, `(const "TEXT" T VALUE)`, `(runtime-checks "K")`. VALUE: `(scalar BITS SIZE)` (the bits as an
unsigned integer, SIZE in bytes), `(ptr ALLOC OFFSET SIZE)`, `(zst)`, `(slice ALLOC LEN)`, `(indirect ALLOC OFFSET)`,
`(unevaluated "PATH" [ARG...] PROMOTED-OR-NIL)`, `(tyconst "TEXT")`. TEXT is what `ConstOperand`'s `Display` prints
(`const 1_i32`, a function's path for a `FnDef`).

Rvalues: `(use OP)`, `(use-noretag OP)`, `(repeat OP "N")`, `(ref :shared|:mut|:fake-deep|:fake-shallow PLACE)`, `(tlsref MUT "PATH")`,
`(rawptr "const|mut" PLACE)`, `(cast "KIND" OP T)` (KIND is the `CastKind` Debug: `IntToInt`, `PointerCoercion(Unsize, Implicit)`...),
`(binop "Add" A B)`, `(unop "Not" A)`, `(discriminant PLACE)`, `(aggregate KIND [OP...])`, `(copy-for-deref PLACE)`, `(wrap-binder OP T)`,
`(reborrow T MUT PLACE)`. Aggregates: `(array T)`, `(tuple)`, `(adt "NAME" "const|fn|struct" [FIELD...] "PATH" VARIANT [ARG...])` (NAME as the
printer writes the variant's path), `(closure "PATH" [ARG...] [UPVAR...] "SPAN")`, `(coroutine ...)`, `(rawptr T MUT)`.

Statements: `(assign PLACE RV)`, `(fake-read "CAUSE" PLACE)`, `(set-discriminant PLACE N)`, `(storage-live L)`, `(storage-dead L)`,
`(place-mention PLACE)`, `(assume OP)`, `(copy-nonoverlapping DST SRC COUNT)`, `(const-eval-counter)`, `(nop)`, `(bidh PLACE)`,
`(raw "TEXT")` (user-type ascriptions, coverage), `(dbg-stmt STMT :dbg ["TEXT"...])` when a statement carries debug infos.

Terminators: `(goto B)`, `(switch OP [[VALUE B]...] OTHERWISE)`, `(resume)`, `(terminate "REASON")`, `(return)`, `(unreachable)`,
`(drop PLACE B UNWIND REPLACE? DROP-B-OR-NIL)`, `(call F [ARG...] DEST TARGET-OR-NIL UNWIND)`, `(tailcall F [ARG...])`,
`(assert OP EXPECTED MSG B UNWIND)`, `(yield V B ARG DROP)`, `(coroutine-drop)`, `(false-edge A B)`, `(false-unwind B UNWIND)`,
`(asm "TEXT" [B...] UNWIND)`. UNWIND: `(continue)`, `(unreachable)`, `(terminate "REASON")`, `(cleanup B)`. MSG: `(bounds-check LEN IDX)`,
`(overflow "Add" A B)`, `(overflow-neg A)`, `(div-zero A)`, `(rem-zero A)`, `(misaligned REQ FOUND)`, `(null-deref)`, `(null-ref)`,
`(invalid-enum A)`, `(resumed "return|panic|drop" "TEXT")`.

## 4. The printer

`arbace.rust.front.mir`: `print-body` (`write_mir_fn`: signature, the scope tree with debug infos and local declarations, blocks, the
allocations text) and `print-dump` (the three header lines and rustc's separators: a blank line between items and before each promoted,
`// MIR FOR CTFE` before a CTFE body). A terminator's successors are printed as `fmt` does (`-> bb1`, `-> [return: bb1, unwind continue]`),
from the labels `fmt_successor_labels` gives. What the printer does not own is the TEXT of types and constants and the allocations'
bytes: they are rustc's renderings carried in the forms. RF4-RF10 replace the type printer and the constant printer by Arbace's own.

## 5. The oracle's two texts

`rsdump rustc --mir` writes `write_mir_pretty`'s text with paths printed in full (the helper's choice, RR6); rustc's own
`-Zunpretty=mir` trims paths (`Arguments` for `std::fmt::Arguments`). The two agree after removing module prefixes on 73 of 74
programs of the synthetic corpus and the samples; the one that differs is a closure's name. The check judges against the helper's.

## 5b. What a monomorphic dump adds (`--mono-mir`, RF3)

`rsdump rustc --mono-mir FILE` writes the bodies of the mono items rustc collects, each `instance_mir` instantiated at its
arguments (no `(param ...)` types remain), and the facts the back half takes as data until Arbace's own answers exist:

- a body's `:symbol` (the v0 name), `:track-caller true`, `:spread-arg N` (a `rust-call` function's tuple argument), `:scopes`
  with `[PARENT INLINED-TEXT INLINED-PARENT CALLSITE-LOC TRACK-CALLER]`, `:alloc-data [["allocN" {...}]...]`: `{:kind :memory :align
  :mutable :bytes "HEX" :init "01.." :relocs [[OFFSET "allocM"]...]}`, `{:kind :fn :symbol}`, `{:kind :static :symbol}` (a vtable is a
  `:memory` entry with `:vtable "TEXT"`);
- a call is `(call F [ARG...] DEST TARGET UNWIND CALLEE FN-LOC SCOPE)`: CALLEE `(callee {:kind K :symbol S :def "PATH" :can-unwind B
  :track-caller B [:intrinsic NAME] [:external true]})`, K one of `item`, `intrinsic`, `virtual`, `fnptr-shim`, `closure-once-shim`,
  `vtable-shim`, `reify-shim`, `clone-shim`, `drop-glue`, `drop-glue-empty` (a no-op), `unresolved`; FN-LOC the call's `(loc FILE LINE COL)`;
  a drop is `(drop PLACE B UNWIND REPLACE? DROP-B GLUE)` with GLUE `(glue {:symbol S})` or nil; an assert has `LOC SCOPE` after its
  unwind; constants of function-item type carry `:fn-symbol S`, unevaluated constants arrive evaluated, an unsizing cast to a trait object
  `:vtable "allocN"`, a closure coerced to a function pointer `:fn-symbol S`;
- after the bodies: `(mir/layouts [["TYPE-TEXT" {:size :align :abi :fields :variants [:discrs] :uninhabited}]...])` (`:abi`
  `(scalar S)`, `(pair A B B-OFFSET)`, `(memory SIZED)`; a scalar `(int BITS SIGNED [LO HI])`, `(ptr [LO HI])`, `(float BITS [LO HI])`;
  `:fields` the offsets, `(array STRIDE COUNT)`, `(union N)`, `(primitive)`; `:variants` `(single I)`, `(empty)` or `(multiple {:tag S
  :tag-field N :encoding (direct)|(niche UNTAGGED FIRST LAST START) :variants [{:offsets :uninhabited}...]})`), `(mir/langsyms
  {"panic_const_add_overflow" "SYMBOL"...})` and `(mir/entry {:main S :lang-start S :sigpipe N})`; `(mir/external ...)` and
  `(mir/static ...)` stand for items with no MIR here.

## 6. Limits of this version

No `built`/`promoted` phases as data; types' structure is partial (`fnptr`, `dyn`, `alias` carry only their text); allocations are
text; inline assembly, coroutines' layouts, coverage and user-type annotations are `raw`. Each is a class of the check, not an error.
