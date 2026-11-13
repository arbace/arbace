# Wasm forms: the specification

The forms repr/wasm reads WebAssembly into and prints back from (doc/wasm/WASM-SURVEY.md §4.2, WAS2 (a), the user 2026-11-13;
"As built: WA1", "As built: WA2"). One vocabulary for both formats: a module read from its binary and the same module read from its
text are equal forms (the cross-format judge, §6). Read with the Clojure reader.

## 1. The `.ae` text

`bin/repr-wasm read FILE.wasm` writes:

- a header form, `(wasm/unit {:format :binary})`;
- then the module's items, one a line, in the binary's order (§2), a function's instructions one a line (nested blocks indented by
  their depth, at most 32 spaces);
- in mode :full (`--full`), the layer (§5) as the last line, `#_(wasm/layer {...})` (a discarded form: the items read alone without it).

`bin/repr-wasm print [--full] FILE.ae -o OUT.wasm` encodes the items, canonically or with the layer.

## 2. Items

Each item is a list headed by a symbol of the `wasm` namespace; an index is the field's index in its index space (imports first, as
the binary numbers them).

| item | meaning |
|---|---|
| `(wasm/type I COMP)` | a type: COMP is `(func [PARAMS] [RESULTS])`, `(struct [FIELD...])`, `(array FIELD)`, or `(sub {:final B :supers [I...]} COMP)`; FIELD `(field STORAGE MUT?)` (STORAGE a value type, `i8` or `i16`) |
| `(wasm/rec I [TYPE...])` | a recursive group whose first type is I |
| `(wasm/import "MOD" "NAME" DESC)` | DESC `(func I {:type T})`, `(table I {:type R :min N :max N :i64 true})`, `(memory I {:min N :max N :i64 true})`, `(global I {:type V :mut B})`, `(tag I {:type T})` |
| `(wasm/func I {:type T :locals [V...]} INSTR...)` | a function: its type index, its locals (flat, the binary's grouping in the layer) and its body |
| `(wasm/table I {:type R :min N :max N :i64 true :init EXPR})` | a table (`:init`: Wasm 3.0's initializer) |
| `(wasm/memory I {:min N :max N :i64 true})` | a memory |
| `(wasm/tag I {:type T})` | an exception tag |
| `(wasm/global I {:type V :mut B} EXPR)` | a global and its initializer |
| `(wasm/export "NAME" (KIND I))` | KIND `func`, `table`, `memory`, `global`, `tag` |
| `(wasm/start I)` | the start function |
| `(wasm/elem I MODE)` | an element segment: MODE a map, one of `:active {:table I :offset EXPR}`, `:passive true`, `:declare true`, with `:type R` and either `:funcs [I...]` (function indices, type `(ref func)`) or `:exprs [EXPR...]` |
| `(wasm/datacount N)` | the binary's data count section |
| `(wasm/data I MODE (hex "..."))` | a data segment: MODE `{:active {:memory I :offset EXPR}}` or `{:passive true}`; its bytes |
| `(wasm/custom "NAME" {:after S} (hex "..."))` | a custom section, after the section S (a keyword of §3, or nil: before every section) |
| `(wasm/names {:after S :module "NAME"})` | the name section where it stands, its names on the functions: `:name` and `:local-names {I "NAME"}` in a `wasm/func`'s map or an imported function's DESC map |

EXPR is a vector of instructions. Bytes are `(hex "...")`, lower-case, two digits a byte.

Value types are symbols (`i32`, `i64`, `f32`, `f64`, `v128`, and the reference shorthands `funcref`, `externref`, `anyref`, `eqref`,
`i31ref`, `structref`, `arrayref`, `exnref`, `nullref`, `nullfuncref`, `nullexternref`, `nullexnref`) or `(ref HEAP)`, `(ref null HEAP)`,
HEAP an abstract heap type symbol (`func`, `extern`, `any`, `eq`, `i31`, `struct`, `array`, `exn`, `none`, `nofunc`, `noextern`,
`noexn`) or a type index.

## 3. Instructions

An instruction is a list headed by its text name (`(i32.add)`, `(local.get 0)`); a structured one holds its body as vectors:
`(block BT [INSTR...])`, `(loop BT [...])`, `(if BT [THEN...])` or `(if BT [THEN...] [ELSE...])` (the binary's `else` present or not),
`(try_table BT [CATCH...] [INSTR...])` with CATCH `(catch TAG L)`, `(catch_ref TAG L)`, `(catch_all L)`, `(catch_all_ref L)`. BT is
`{}` (no result), `{:bt [V]}` (one result) or `{:bt T}` (a type index).

### 3.1 The table

`arbace/repr/wasm/ops.ae` is generated (`bin/repr-wasm ops gen`; `check` regenerates and compares) from the specification's
reference interpreter at the pin (`test/wasm/corpora.edn`'s `:spec`, WebAssembly/spec tag wg-3.0): `interpreter/binary/decode.ml`'s
`instr` gives each opcode, the constructor and the immediates it reads in order, `interpreter/text/lexer.mll` each constructor's
text name. 493 rows: Wasm 3.0's every instruction but the four structured ones, among them SIMD128 and relaxed SIMD (`0xfd`), the
numeric, bulk memory and table instructions (`0xfc`), GC (`0xfb`), tail calls, typed function references and exceptions.
`test/wasm/go_ops.ae` checks every opcode Go's wasm assembler can emit (`cmd/internal/obj/wasm`'s a.out.go and writeOpcode: 446) is
in the table under its name (five by Go's own spellings).

### 3.2 Immediates (the table's kinds)

| kind | form | binary |
|---|---|---|
| `:none` | `(OP)` | |
| `:idx` | `(OP I)` | u32 |
| `:idx2` | `(OP X Y)` | X then Y (memory.copy, table.copy, array.copy, array.new_data, ...) |
| `:idx2-rev` | `(OP X Y)` | Y then X (call_indirect TABLE TYPE, return_call_indirect, memory.init MEM DATA, table.init TABLE ELEM) |
| `:idx-field` | `(OP TYPE FIELD)` | struct.get and its kin |
| `:idx-u32` | `(OP TYPE N)` | array.new_fixed |
| `:mem` | `(OP {:align A :offset O})`, `:memory M` when the flags' bit 6 names one | memarg |
| `:mem-lane` | `(OP MEMARG LANE)` | |
| `:lane` | `(OP LANE)` | a byte |
| `:shuffle` | `(OP [16 LANES])` | 16 bytes |
| `:br-table` | `(br_table [L...] DEFAULT)` | |
| `:heap` | `(ref.null HEAP)` | |
| `:ref-test`, `:ref-null-test` | `(ref.test (ref HEAP))`, `(ref.test (ref null HEAP))` (ref.cast alike) | one opcode each |
| `:i32`, `:i64` | `(OP N)`, N signed | sLEB128 |
| `:f32`, `:f64` | `(OP D)`, D a double (an f32 exactly), `##Inf`, `##-Inf`, or `(:nan BITS)` for every NaN (its bits whole) | little-endian bits |
| `:v128` | `(v128.const (hex "32 digits"))` | 16 bytes |
| `:select-t` | `(select [V...])` (`(select)` the untyped one) | |
| `:br-on-cast` | `(br_on_cast L REF1 REF2)` | flags, label, two heap types |

## 4. Decoding

`arbace.repr.wasm.binary/decode` reads the binary grammar in decode.ml's order of reads, with its checks and messages (`unexpected
end of section or function`, `integer representation too long`, `integer too large`, `length out of bounds`, `malformed section id`,
`section size mismatch`, `malformed UTF-8 encoding`, `illegal opcode XX`, `END opcode expected`, `function and code section have
inconsistent lengths`, `data count and data section have inconsistent lengths`, ...): a malformed module throws ex-info
`{:wasm/malformed MESSAGE :at BYTE}`. Validation is not decoding (WA3, `arbace.wasm.valid`).

The name section (subsections 0, 1, 2: the module's, the functions', the locals' names) is read into `wasm/names` and the functions'
`:name` and `:local-names` when its encoding, from those names, is the original's byte for byte (with the layer's `:name-widths`);
else it stays a `wasm/custom "name"`.

## 5. Mode :full: the layer

What the forms do not say and the binary does, so that the original prints back byte for byte:

- `:widths {W [ORD...]}`: the LEB128s whose width W is not their value's minimal one, by their ordinal among the module's LEB128 reads
  (section sizes, counts, indices, immediates, in decode order; `encode` writes them in the same order). Go's linker pads section
  and function body sizes to 5 bytes and its relocated `call` and `i32.const` operands to 5 (P2).
- `:name-widths`: the same for the name section's own LEB128s (its ordinals its own).
- `:empty [S...]`: sections present with no entries.
- `:flags {[:elem I] F, [:data I] F, [:locals FUNC] [[N V]...]}`: an element or data segment's flags when not the canonical encoding
  of its form, a function's local declarations when not grouped canonically (runs of one type).

Canonical encoding: minimal LEB128s, the sections in the specification's order (type, import, function, table, memory, tag, global,
export, start, element, data count, code, data), each present when it has entries, custom sections where their `:after` places
them, the segments' flags the shortest that encode the form.

## 6. The judges (WA1)

`bin/repr-wasm roundtrip` (each module of `test/wasm/corpora.edn`'s corpora), by the module's own verdict: decoded in :full; the
`.ae` text read back to the same items and layer (`ae-differs`, `layer-differs`); printed back with the layer, the original's bytes
(`bytes-differ`); the canonical encoding decoded again to the same items (`forms-differ`) and no layer (`canon-layer`). Results
`test/wasm/roundtrip-results.edn`, classes `test/wasm/divergences-read.edn`, controls `test/wasm/controls.txt`.

## 7. The text format (WA2)

`arbace.repr.wasm.lex` reads the text format's tokens (the specification's lexical grammar: keywords, `$id` and `$"..."`,
strings with `\hh`, `\u{...}` and the named escapes, numbers with `_`, hexadecimal floats, `inf`, `nan`, `nan:0x...`; `;;` and
nested `(; ;)` comments; annotations `(@...)` skipped) into S-expressions. `arbace.repr.wasm.text` reads a module's fields into the
items of §2, identifiers resolved to indices (not kept: WA2), the abbreviations expanded (inline imports and exports; inline type
uses, an undefined function type appended to the types in the order of first use; folded instructions; named params and locals; a
table's inline `(elem ...)` and a memory's inline `(data ...)`; the MVP's element and data spellings); the data count section added
when a function uses `memory.init` or `data.drop`.

`arbace.repr.wasm.print/module-text` prints items as canonical text: flat, numeric indices, explicit `(type N)` uses, every memarg's
`offset=` and `align=`, floats in hexadecimal (exact), data as one string with `\hh` escapes. Custom sections, names and the data
count section have no text here (`print-items`: what the text keeps).

## 8. The testsuite (WA2)

`bin/repr-wasm wast` reads each script of the testsuite's pin and judges each command at the read level (`arbace.repr.wasm.wast`):

- a module (text, `binary`, `quote`, `definition`): read; cross-format (encoded, decoded again, equal); a binary one printed back byte
  for byte; the print level (its canonical text read again to the same `print-items`);
- `assert_malformed`: rejected (the message the script states is not required: WAS8 (a));
- `assert_invalid`: read, its rejection the validator's (WA3, classed);
- the modules of `assert_unlinkable`, `assert_uninstantiable`, `assert_trap`, `assert_exception`: read as modules.

Case ids `testsuite/PATH#K` (K the command's ordinal from 1), results `test/wasm/wast-results.edn`, classes
`test/wasm/divergences-wast.edn`, controls `test/wasm/controls-wast.txt`.
