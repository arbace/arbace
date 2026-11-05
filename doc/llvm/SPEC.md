# LLVM forms: the reader, the printer, mode :full and the verifier

Moved (LL1, 2026-11-05) from doc/cpp/LLVM.md (sections 1 to 6; the lowering, sections 7 to 10, is doc/llvm/LOWER.md), as the LLVM survey's LLS13 decided (doc/llvm/LLVM-CROP-SURVEY.md). Status: built write-ahead in CX6 (doc/cpp/CPP-SURVEY.md, "As built: CX6 (write-ahead)"); the reader grew since (LL3, `llvm-read`), mode `:full` (LL8) and the verifier (LL4).

Pieces:

- `arbace.repr.llvm` (`arbace/repr/llvm.ae`, `arbace/repr/llvm/{read,print,full,verify,intr,mutate,diag}.ae`): IR text -> LLVM forms (`read`), LLVM forms -> IR text (`print`), the forms as `.ae` text (`module-text`), mode `:full` (6.1), the verifier (6.2, 6.2.1, 6.2.2; `intr`: the intrinsics' names and signatures) and its mutants; `bin/repr-llvm read|print|roundtrip|verify|verify-check|mutants`; `bin/llvm-corpus`: llvm/test as a corpus (LL1).

All are tools' namespaces evaluated from the checkout on the tools' host (stage 0); nothing is embedded.

## 1. Principles

1. **The text's grammar, LLParser's (LLVM 23), as clang 23 writes it and LangRef documents it.** One form per top-level
   entity in the text's order; one form per instruction or debug record in its block's order. What the forms do not keep
   is spelling (white space, comments, the `; preds =` and `; guid =` annotations, the spelling of an attribute group's
   members); the judges are the object and the bitcode: `clang -c` of the print equals `clang -c` of the original byte for
   byte, and so does `llvm-as`'s bitcode (§6). Since 2026-11-04 the reader takes every construct of the pin's grammar
   (doc/llvm/LLVM-CROP-SURVEY.md's "Read gaps" closed: debug records and the DI metadata as structured forms, `callbr`,
   Windows EH's pads, module summaries, `ptrtoaddr`, use-list orders, typed pointers, and the corners below); the lowering
   (§7) takes the subset clang writes at `-O0` and refuses the rest in a class.
2. **Plain Clojure syntax**, read by Arbace's reader without data readers. LLVM's sigils that the reader cannot take
   are spelled otherwise: `@x` (which the reader reads as a deref) is `g/x`; names the reader would not read back as a
   symbol are `(% "...")` and `(g "...")`; `#dbg_value` (a dispatch) is `dbg_value`; `^3` (metadata) is `(summary 3)`.
3. **Positional where LLVM is positional, keyword options where LLVM's syntax is optional**: an instruction is its
   opcode, its flags (keywords), its type and operands in the text's order, then `:key value` options (`:align`,
   `:volatile`, `:md` ...) that are written only when present.

## 2. Names

| IR | form |
|---|---|
| `%x`, `%x.addr`, `%0`, `%struct.S` (local values, labels, named types) | the symbols `%x`, `%x.addr`, `%0`, `%struct.S` |
| `%"class.std::basic_ostream"`, any local name the reader would not read back | `(% "class.std::basic_ostream")` |
| `@main`, `@.str` would be `g/.str`: `@_ZN1A1fEv`, `@llvm.memcpy.p0.p0.i64` | `g/main`, `g/_ZN1A1fEv`, `g/llvm.memcpy.p0.p0.i64` |
| `@.str.1`, `@0`, `@"\01foo"` | `(g ".str.1")`, `(g 0)`, `(g "\u0001foo")` |
| `$comdat` | the string `"comdat"` (in `llvm/comdat` and `:comdat`) |
| `!0`, `!llvm.loop` | the symbols `!0`, `!llvm.loop` |
| `#3` (an attribute group) | the integer `3` in an `:attrs` vector |
| `^3` (a summary entry), `readonly ^3`, `writeonly ^3` | `(summary 3)`, `(summary 3 :readonly)`, `(summary 3 :writeonly)` |

A global's name is a plain `g/NAME` when it starts with a letter, `$` or `_` and holds neither `..` nor a final `.`;
a string's bytes are chars 0-255 (`\XX` escapes decoded).

## 3. Types

| IR | form |
|---|---|
| `void`, `i1`, `i32`, `i128`, `float`, `double`, `x86_fp80`, `fp128`, `half`, `ptr`, `label`, `metadata`, `token` | the symbol |
| `ptr addrspace(1)`, `ptr addrspace("A")` (a data layout's name for it) | `(ptr 1)`, `(ptr "A")` |
| `[4 x i8]`, `<4 x i32>`, `<vscale x 4 x i32>` | `(array 4 i8)`, `(vector 4 i32)`, `(vscale 4 i32)` |
| `{ i32, ptr }`, `{}`, `<{ i8, i32 }>` | `(struct i32 ptr)`, `(struct)`, `(packed i8 i32)` |
| `i32 (ptr, ...)` | `(fn i32 [ptr &])` |
| `%struct.S` | `%struct.S` (a named type: `(llvm/type %struct.S (struct ...))` or `(llvm/type %struct.S opaque)`) |
| `target("x", i8, 1)` | `(target "x" i8 1)` |
| `i8*`, `i8 addrspace(1)*`, `void (i32)*` (typed pointers) | `(* i8)`, `(* i8 1)`, `(* (fn void [i32]))` |

Typed pointers are LLParser's upgrade (it reads `T*` as `ptr`): read and printed as written, the bitcode judging (clang 23
writes opaque pointers only; the lowering does not take them).

## 4. Values and constants

| IR | form |
|---|---|
| `42`, `-1`, `true`, `false` | `42`, `-1`, `true`, `false` (integers beyond a long as BigInts) |
| `1.500000e+00`, `0x3FB999999999999A`, `0xK4000C000000000000000` | `(fp "1.500000e+00")`, `(fp "0x3FB999999999999A")`, `(fp "0xK4000C000000000000000")` (the spelling: LLVM's floats are exact in it) |
| `null`, `undef`, `poison`, `zeroinitializer`, `none` | the symbols |
| `c"ab\00"` | `(c-string "ab\u0000")` |
| `{ i32 1, ptr null }`, `<{ ... }>`, `[i32 1, i32 2]`, `<i32 1, i32 2>` | `(struct [i32 1] [ptr null])`, `(packed ...)`, `(array [i32 1] [i32 2])`, `(vector ...)` |
| `getelementptr inbounds (T, ptr @g, i64 0, i32 1)` | `(getelementptr :inbounds T [ptr g/g] [i64 0] [i32 1])`; `inrange(-16, 8)` is the flag `(:inrange "-16, 8")` |
| `ptrtoint (ptr @f to i64)`, `ptrtoaddr (ptr @f to i64)`, `add (i64 a, i64 b)` | `(ptrtoint ptr g/f i64)`, `(ptrtoaddr ptr g/f i64)`, `(add i64 a b)` (a constant cast or binary operator has its instruction's form) |
| `extractelement (<2 x i64> v, i32 0)`, `insertelement (...)`, `shufflevector (...)` | `(extractelement [(vector 2 i64) v] [i32 0])` ...: the typed operands |
| `blockaddress(@f, %bb)`, `dso_local_equivalent @f`, `no_cfi @f`, `splat (i32 1)` | `(blockaddress g/f %bb)`, `(dso_local_equivalent g/f)`, `(no_cfi g/f)`, `(splat [i32 1])` |
| `ptrauth (ptr @f, i32 0, i64 1234)` | `(ptrauth [ptr g/f] [i32 0] [i64 1234])` |
| `asm sideeffect "nop", ""` | `(asm "nop" "" :sideeffect)` |

A typed operand `T v` is the vector `[T v]`; a call argument or a parameter with attributes `[T attr... v]`.

**Attributes**: a bare word is a keyword (`:nounwind`, `:noundef`); `align 8` is `(:align 8)`; `word(ARGS)` is
`(:word TYPE)` for the type-taking ones (`byval`, `sret`, `byref`, `inalloca`, `preallocated`, `elementtype`) and
`(:word "ARGS")` (the text) for the others (`dereferenceable(8)`, `memory(argmem: readwrite)`, `denormal_fpenv(dynamic,
float: preservesign)`); `word=N` is `(:word= N)`; a string attribute `"k"="v"` is `["k" "v"]`, `"k"` is `["k"]`, also on
a parameter, an argument or a return value. A call site's function attributes are its group numbers and its inline
attributes, `:attrs [5 :nounwind ["k" "v"]]` (clang writes groups only).

**Metadata**: `!N`, `!name` symbols; `!"s"` is `(! "s")`; `!{...}` is `(! [...])`; a typed constant in a node is `[T v]`.
A specialized node (LLS19 (a)) is a list headed by its name with its `!`, its keyed fields in the text's order:
`!DILocation(line: 3, column: 5, scope: !10)` is `(!DILocation :line 3 :column 5 :scope !10)`. A field's value is an
integer, `true` or `false`, a string (an MDString field: `name: "x"` is `"x"`, where `!"x"` is `(! "x")`), `null`, a
word as a symbol (`tag: DW_TAG_member`, `emissionKind: FullDebug`, `nameTableKind: None`), metadata (a reference or an
inline node), a typed constant (`extraData: i64 0`, `value: i32 7` as `[i64 0]`, `[i32 7]`), words and integers joined
by `|` as `(| DIFlagPrototyped DIFlagArtificial 1048576)`, and GenericDINode's `operands: {!1, null}` as `(list !1 null)`.
Three kinds take positional operands: `!DIExpression(DW_OP_LLVM_fragment, 0, 32)` is `(!DIExpression DW_OP_LLVM_fragment
0 32)` (the `DW_OP_` and `DW_ATE_` words symbols), `!DIArgList(i32 %a, i32 %b)` is `(!DIArgList [i32 %a] [i32 %b])`,
`!DIAssignID()` is `(!DIAssignID)`. The fields are not checked against the node's kind (LLParser and the verifier do
that): the forms keep what the text writes, and every kind of LLVM 23 reads alike (`DICompileUnit` ... `DISubrangeType`,
`DIFixedPointType`, the fields of later releases among them).

## 5. Instructions and the top level

```clojure
(= %r (add :nsw i32 %a 1))                       ; %r = add nsw i32 %a, 1
(= %c (icmp :slt i32 %a %b))                     ; icmp slt (samesign before the predicate: (icmp :samesign :ult ...))
(= %d (sext i8 %x i32))                          ; sext i8 %x to i32
(= %p (alloca %struct.S :align 8))
(= %v (load i32 [ptr %p] :align 4 :volatile true))
(store [i32 %v] [ptr %p] :align 4)
(= %q (getelementptr :inbounds %struct.S [ptr %p] [i32 0] [i32 1]))
(= %m (phi i32 [%a %bb1] [0 %bb2]))
(= %s (select [i1 %c] [i32 %a] [i32 %b]))
(= %r (call i32 g/f [[i32 :noundef %a]] :attrs [5]))
(= %r (call (fn i32 [ptr &]) g/printf [[ptr :noundef (g ".str")] [i32 %v]]))
(= %r (invoke ptr g/_Znwm [[i64 8]] :to %ok :unwind %lpad :attrs [12]))
(= %lp (landingpad (struct ptr i32) :cleanup (catch ptr g/_ZTIi) (catch ptr null)))
(br i1 %c %then %else) (br %next) (switch i32 %x %default [[i32 1 %one] [i32 2 %two]])
(ret i32 0) (ret void) (resume (struct ptr i32) %lp) (unreachable)
;; asm goto: callbr ... to label %ft [label %a, label %b]
(= %r (callbr i32 (asm "..." "=r,!i,!i") [[i32 %x]] :to %ft :indirect [%a %b] :attrs [3]))
;; Windows EH (funclets)
(= %cs (catchswitch none [%c1 %c2] :unwind %outer))  ; catchswitch within none [label %c1, label %c2] unwind label %outer
(= %p (catchpad %cs [[ptr null] [i32 64] [ptr null]])) ; catchpad within %cs [ptr null, i32 64, ptr null]
(= %cl (cleanuppad none []))                         ; cleanuppad within none []
(catchret %p :to %done)                              ; catchret from %p to label %done
(cleanupret %cl :unwind caller)                      ; cleanupret from %cl unwind to caller
;; a debug record, before the instruction it is attached to
(dbg_value [i32 %x] !12 (!DIExpression) !15)         ; #dbg_value(i32 %x, !12, !DIExpression(), !15)
;; metadata attachments, in order:
(br %for.cond :md [!llvm.loop !8])
```

The rest: `fneg`, `fcmp`, every cast (`ptrtoaddr` among them), `extractvalue`/`insertvalue` (indices after the typed
operands), `extractelement`, `insertelement`, `shufflevector`, `freeze`, `va_arg`, `fence ORDERING`, `atomicrmw OP [ptr p]
[T v] ORDERING` (`elementwise` is `:elementwise true`), `cmpxchg [ptr p] [T c] [T n] SUCCESS FAILURE`, `indirectbr`, `tail
call` (`:tail :tail`), atomic loads and stores (`:atomic :seq_cst`, `:syncscope "x"`). `unwind to caller` (`catchswitch`,
`cleanupret`) is the symbol `caller`, `unwind label %bb` the label. A debug record (`#dbg_value`, `#dbg_declare`,
`#dbg_assign`, `#dbg_label`, `#dbg_declare_value`) is `(dbg_KIND operand...)`, its operands metadata (§4: the value
`[i32 %x]`, `!N`, an inline `(!DIExpression ...)` or `(!DIArgList ...)`), an item of its block in the text's order.

Top level: `(llvm/source-filename "x.cpp")`, `(llvm/datalayout "...")`, `(llvm/triple "...")`, `(llvm/type NAME T)`,
`(llvm/comdat "name" :any)`, `(llvm/global NAME T INIT? :linkage :internal :constant true :align 4 ...)`, `(llvm/alias
...)`, `(llvm/declare NAME RET [PARAMS] :attrs [3] ...)`, `(llvm/define NAME RET [PARAMS] :linkage :linkonce_odr :comdat
true :personality [ptr g/__gxx_personality_v0] :attrs [0] :body [(block LABEL inst...) ...])` (the entry block's
label `nil` when the text does not write it), `(llvm/attributes 0 [...])`, `(llvm/meta !5 MD :distinct true)`,
`(llvm/named-meta !llvm.ident [!5])`, `(llvm/module-asm "...")`. A parameter is `[T attr... %name]` (`[T attr...]`
unnamed in a declare), `&` for `...`. Besides:

- A global's properties (`, section "s"`, `, partition "p"`, `, comdat`, `, align N`, `, code_model "large"`, the
  sanitizer words `, no_sanitize_address` ... as `:no_sanitize_address true`) are options in the text's order, then
  `:attrs` (its groups and inline attributes, printed after its metadata as LLParser requires) and `:md` (its
  attachments, wherever the text puts them among the properties). An alias's or an ifunc's `:partition`; an aliasee that
  the text writes without its type (`bitcast`, `getelementptr`, `addrspacecast`, `inttoptr`: LLParser implies it) is the
  constant alone, `(llvm/alias g/a ptr (getelementptr :inbounds ...))`.
- A declaration's metadata, written before its header (`declare !dbg !5 void @f()`), is its `:md`, as a definition's
  (after its header) is; a function's `prefalign(N)` is `:prefalign N`; a calling convention is its word as a string
  (`:cconv "fastcc"`, `"cc 10"`, `"riscv_vls_cc(256)"`).
- Use-list orders: `uselistorder ptr @g, { 1, 0, 2 }` is `(llvm/uselistorder [ptr g/g] [1 0 2])`; a function's, after its
  last block, its define's `:uselistorder [(uselistorder [i32 %x] [1 0]) ...]` after `:body`. (`uselistorder_bb` is not
  LLVM 23's.)
- A module summary entry `^N = KIND: VALUE` (`module`, `gv`, `typeid`, `typeidCompatibleVTable`, `flags`, `blockcount`)
  is `(llvm/summary N :KIND VALUE)`; a summary value is an integer, a string, a word (a symbol: `linkage: internal`), a
  reference (`(summary N)`, §2), a tuple `(key: v, ..., v ...)` as a vector of `:key v` pairs and bare values in the
  text's order, and a parameter access's `[a, b]` as `(range a b)`: `^0 = module: (path: "x.o", hash: (0, 0, 0, 0, 0))`
  is `(llvm/summary 0 :module [:path "x.o" :hash [0 0 0 0 0]])`, `refs: (readonly ^4)` is `:refs [(summary 4
  :readonly)]`. Summaries are data: read and printed, never lowered.

## 6. The round trip

`bin/repr-llvm roundtrip LIST WORK` (levels 1 and 2 are also `bin/cppi lower-check`'s first, on x86-64 Linux IR, whose
triple is the host's):

1. **Forms**: the forms written as `.ae` text and read back are equal; the print read again gives the same forms.
2. **Object**: `clang -c` of the print equals `clang -c` of the original, byte for byte, each built for the module's
   own `target triple` (`-target`; clang would otherwise build it for the host's) with `-mno-incremental-linker-compatible`
   (a COFF object without its time stamp). (`clang -c` of the original IR, not of the C++ source: the IR is the eval's
   input.)
3. **Bitcode**: LLVM 23's `llvm-as` (`/usr/lib/llvm23/bin/llvm-as`, `REPR_LLVM_AS`) gives the print the original's
   bitcode, byte for byte: the judge that sees module summaries and the metadata no object holds (LLS5 (a)'s canonical
   text follows from it).

A judge is `same`, `differs` (the outputs differ, or the print does not build where the original does) or `none` (the
original does not build: a back end that cannot lower the module, `ptrauth` on x86-64); a file passes when its forms are
equal, no judge differs and one judge is `same`.

### 6.1 Mode `:full` (LLS18 (a), LL8)

`bin/repr-llvm read --full FILE.ll [-o OUT.ae]` reads a module with its text's layer, `print --full FILE.ae [-o OUT.ll]`
prints it back, the original byte for byte (`arbace.repr.llvm.full`). The forms are the plain mode's, unchanged (the same
reader, the same `.ae` text); the layer is the `.ae`'s last line, `#_(llvm/layer {...})`, which the reader skips, so a
`:full` `.ae` is also a plain one. The layer is kept item by item, an item being a top-level form, or in a definition its
header up to `{`, each label, instruction, debug record and use-list order, and its `}`; for each item it holds what the
canonical print does not write:

- the **gap** before it (white space and comments: `; ModuleID`, `; Function Attrs:`, `; preds = %2`, a `RUN:` line, a
  hand-written note, blank lines), when it is not the canonical layout's;
- the item's **text**, when its spelling is not the printer's: spacing inside it, a name quoted that need not be
  (`@"plain"`) or one the printer quotes otherwise (clang quotes `$`), an escape, an optional word (`external` on a
  declaration, `default`), a global's properties or metadata in another order, `{ i32,i32 }`, `!{ !1, !1 }`, a typed
  pointer, `#dbg_value` at another indentation.

What LLVM lets a text write in several ways and the forms already distinguish stays in the forms: numbered and named
values, an instruction written without `%N =` and a block without a label (LLParser numbers both: the form has no `=`,
the block's label is nil), hex and decimal floats (`(fp "0x400921FB54442D18")`, `(fp "1.5")`), `u0x`/`s0x` integers,
attribute groups and inline attributes, `align` written or not. A kept text carries the hash of the item's canonical print:
when the form has changed since (an edited `.ae`), the item prints canonically; a form whose item count changed prints
canonically whole, its first gap kept. A layer is `{:items [E ...] :end S}`, an `E` per top-level form, nil when all its
items are canonical, else an entry per item: nil, the gap alone (a string), or `[GAP TEXT HASH]`; `:end` the text after
the last item. The printer is built from the same items (`form-items`, `module-gap`), so the canonical text is the layer's
empty case: the plain print of the 278 modules below is byte for byte what it was.

`read -o` and `print` now write and read the `.ae` text as Latin-1 (a string's bytes are chars 0-255), as the round
trip always did. The reader changed with it in one place: a block that follows a terminator without a label starts a new block (label
nil), as LLParser parses it; before, its instructions joined the previous block. No module of the lists has one.

`bin/repr-llvm roundtrip --text LIST WORK` (`WORK/full/NAME.ae`, `WORK/text.tsv`: NAME, status, detail, forms with an
entry, gaps, texts): each file read in `:full` (its forms the plain reader's), its `.ae` text read back (forms and layer
equal), printed back (the original byte for byte), and the plain print read in `:full` with an empty layer and printed
back to itself (the plain mode idempotent). On 2026-11-04 over the round trip's lists (278 modules of `-O0`, `-O2`, `-g`,
LTO and Windows IR from C, C++ and Rust, the six hand-written corner modules) and four hand-written modules with
comments, blank lines, odd spacing, numbered and unnamed values, unlabelled blocks, hex floats and laid-out metadata: **282
of 282 printed back byte for byte**, 4,322 top-level forms with a layer entry, 7,130 gaps and 18 texts kept (clang writes
the canonical spelling but for the gaps and its quoted `$` names), 29 s at 4 threads. The four hand-written modules pass
the plain round trip's judges too (forms, objects and bitcode equal).

### 6.2 The verifier (LL4, LLS6 (a))

`bin/repr-llvm verify FILE.ll|FILE.ae...` (`arbace.repr.llvm.verify`): what `opt -passes=verify` prints for each invalid
file, line for line (§6.2.2; until LL4c, each invalid file's first error in LLVM's words with its rule), exit 1 when one is
invalid. It runs in two phases, as the pin's `opt -passes=verify` does: LLParser's checks beyond the grammar (`llvm/lib/AsmParser/LLParser.cpp`), in the text's order and
stopping at the first; then, when they pass, the Verifier's rules (`llvm/lib/IR/Verifier.cpp`), each function in the
module's order (visitFunction, then each block's visitBasicBlock and its instructions' visitors), then the module's, all of
its failed visits. The messages of the table are LLVM's words; their positions and the values opt prints after them are §6.2.2's.

Ported (the namespace's `rules`):

| phase | rule | LLVM's message |
|---|---|---|
| parser | `value-type`, `not-a-block` | `'%x' defined with type 'i32' but expected 'i64'`; `'%x' is not a basic block` |
| | `forward-type` | `instruction forward referenced with type 'T'` |
| | `numbering` | `instruction expected to be numbered '%N' or greater` (`argument`, `label` alike; LLVM 23 allows gaps) |
| | `redefinition` | `multiple definition of local value named 'x'`, `redefinition of global '@g'`, `invalid redefinition of function 'f'` |
| | `undefined-value`, `undefined-global`, `undefined-metadata` | `use of undefined value '%x'` (a function's end: names in byte order, then numbers), `'@g'` and `'!N'` at the module's end (`llvm.*` names exempt: LLVM 23 declares them) |
| | `global-type`, `int-constant`, `fp-constant`, `constant-type`, `null` | `global variable reference must have pointer type`, `integer/byte constant must have integer/byte type`, `floating-point constant invalid for type`, `constant expression type mismatch: got type 'i1' but expected 'T'`, `null must be a pointer type` |
| | `void-name`, `terminator`, `syntax` | `instructions returning void cannot have a name`; `expected instruction opcode` (a block without a terminator: LLParser reads a block up to one); `expected value token` (a phi without entries), `function body requires at least one basic block` |
| | `operand-type`, `icmp`, `fcmp`, `cast`, `select`, `phi`, `gep`, `load`, `store`, `atomic-align`, `atomicrmw`, `cmpxchg`, `extractvalue`, `insertvalue`, `vector` | the operand rules of each instruction (`invalid operand type for instruction`, `invalid cast opcode for cast from 'i64' to 'i32'` with CastInst::castIsValid, `select condition must be i1 or <n x i1>`, `load operand must be a pointer to a first class type`, ...) |
| | `call-argument`, `call-arguments`, `ret`, `br`, `switch` | `argument is not of expected type 'T'`, `too many arguments specified`; `value doesn't match function result type 'T'`; `branch condition must have 'i1' type`; `switch condition must have integer type`, `duplicate case value in switch` |
| | `align` | `alignment is not a power of two`, `huge alignments are not supported yet` |
| | `linkage`, `visibility`, `dll` | `invalid linkage for function declaration` / `definition`, `invalid function linkage type`, `symbol with local linkage must have default visibility`, `... cannot have a DLL storage class` |
| | `dbg-record` | a debug record's operand count: `Expected ',' here`, `Expected ')' here` |
| verifier | `entry-predecessors` | `Entry block to function must not have predecessors!` |
| | `phi-predecessors`, `phi-duplicate`, `phi-entries`, `phi-grouped` | `PHINode should have one entry for each predecessor of its parent basic block!`, `PHI node has multiple entries for the same basic block with different incoming values!`, `PHI node entries do not match predecessors!`, `PHI nodes not grouped at top of basic block!` |
| | `dominance`, `self-reference` | `Instruction does not dominate all uses!` (dominators by Cooper, Harvey and Kennedy; a use in an unreachable block dominated, a definition in one dominating nothing, an invoke's result on its normal edge, a phi's use at its incoming block's end; callbr's results are available in every destination); `Only PHI nodes may reference their own value!` |
| | `terminator-middle` | `Terminator found in the middle of a basic block!` (forms written by hand: the reader splits such a block) |
| | `landingpad`, `eh-pad-entry`, `unwind-dest` | the landingpad's rules (a clause or cleanup, only an invoke's unwind edge, a personality, the first non-PHI, one type in a function); `EH pad cannot be in entry block.`; `The unwind destination does not have an exception handling instruction!` |
| | `intrinsic-call`, `intrinsic-user`, `intrinsic-signature`, `intrinsic-defined`, `lifetime` | `Intrinsic called with incompatible signature`; `Invalid user of intrinsic instruction!` (at the declaration's visit; the assume-like intrinsics' calls ignored, as hasAddressTaken does); a declaration against a table of about 40 base names with their overloads, `intrinsic has incorrect number of args. Expected N, but got M`, `intrinsic argument K type (matching overload type J) expected T, but got U`, `intrinsic return type expected void, but got i32`; `llvm intrinsics cannot be defined!`; `llvm.lifetime.start/end can only be used on alloca or poison` (lifetime's old two-argument form accepted, as AutoUpgrade takes it) |
| | `md-range`, `md-nonnull`, `md-align`, `md-dereferenceable` | metadata attachments' placement and types (`Ranges are only for loads, calls and invokes!`, `nonnull applies only to pointer types`, ...) |
| | `comdat-declaration` | `Declaration may not be in a Comdat!` |

Not ported: LLParser's syntax errors (the reader refuses a text outside the grammar with its own message; the positions of the
errors that are ported are §6.2.2's); GC statepoints and the per-intrinsic checks of visitIntrinsicCall beyond `lifetime`, `ptrmask` and the
signatures of the table (the coroutine, statepoint, vector-predication, matrix and constrained-FP intrinsics have no
entry); Windows EH's coloring (the nesting and sibling rules of §6.2.2 are ported without it); swifterror's rules for
loads and stores (its calls' are §6.2.2's), the `memory(...)` attribute's syntax; atomic orderings' constraints; DI nodes beyond the list of §6.2.1; aliases' and ifuncs' rules, global
ctors and dtors, use-list orders' validity, `blockaddress`; AutoUpgrade beyond the old intrinsic forms of
`old-form-pattern`. Known divergences: a label defined twice (`a:` ... `a:`) is a second block here, LLParser appends
the second's instructions to the first's block; opt aborts (`PLEASE submit a bug report`) on a call to an intrinsic
declared with a mismatching signature and fewer arguments, and AutoUpgrade rewrites a `lifetime` declared with an extra
parameter; an intrinsic's declaration and its calls are verified in module order, so a mismatching declaration after
its calls is reported after theirs, as opt does, but the table's patterns for a few intrinsics (`ptrmask`'s return) are
looser than Intrinsics.td's.

### 6.2.1 Attributes, calls, intrinsics, funclets, metadata and debug info (LL4b)

The rules added on 2026-11-04 (branch `llvm-verify2`), after LLVM 23's `Verifier.cpp` and `LLParser.cpp` as the pin's
`libLLVM-23.so` shows them (its strings, in code order, name every message; the order and the conditions were found by
probing opt: `test/llvm/probes/`). Rules are named `phase/name`; a message is opt's, and a rule that opt reports as a
warning (debug info) has the verdict `debug`.

| family | rules | LLVM's message (examples) |
|---|---|---|
| attributes, parser | `parser/attr-placement`, `parser/attr-value`, `parser/sret-return`, `parser/arg-type`, `parser/di-field` | `this attribute does not apply to functions` / `return values` / `parameters` (Attributes.td's applicability: `nonnull` on a function, `noinline` on a parameter, in a header, a call or an attribute group); `byte count specified must be non-zero`, `stack alignment is not a power of two`, `Invalid (unordered or overlapping) range list`, `invalid mask value for 'nofpclass'`, `unknown allockind X`, `void type only allowed for function results`; `functions with 'sret' argument must return void`; `'builtin' attribute not valid on function` |
| attributes, verifier | `verifier/attr-type`, `attr-pair`, `attr-value`, `attr-misc`, `attr-placement` | `Attribute 'sret(i32)' applied to incompatible type!` (the pointer-only attributes on a non-pointer, `zeroext`/`signext`/`allocalign` on a non-integer, `range` on a non-integer or of another width, `nofpclass` on a non-float, vectors of pointers included); `Attributes 'zeroext and signext' are incompatible!` and the like (`readnone`/`readonly`/`writeonly`/`writable`, `inalloca`/`readonly`, `sret`/`returned`, `noinline`/`alwaysinline`, `optnone` and `noinline`/`optsize`/`minsize`/`optdebug`); `Attributes 'byval', 'inalloca', 'preallocated', 'inreg', 'nest', 'byref', and 'sret' are incompatible!`; `Attribute 'immarg' is incompatible with other attributes except the 'range' attribute`; `Attribute 'byval' does not support unsized types!`; `More than one parameter has attribute nest!` / `returned`, `Incompatible argument and return types for 'returned' attribute`, `Cannot have multiple 'sret' parameters!` / `swiftself` / `swiftasync` / `swifterror`, `Attribute 'sret' is not on first or second parameter!`, `inalloca isn't on the last parameter!`; `immarg attribute only applies to intrinsics`, `Attribute 'elementtype' can only be applied to intrinsics and inline asm.`; the function attributes' values (`'vscale_range' minimum must be power-of-two value`, `'allocsize' element size argument is out of bounds`, `invalid value for 'frame-pointer' attribute: X`, `\"patchable-function-entry\" takes an unsigned integer: x`, `'allockind()' requires exactly one of alloc, realloc, and free`, `Attribute 'jumptable' requires 'unnamed_addr'`); an intrinsic declaration whose type is the intrinsic's has the intrinsic's own attributes, the text's are not checked (Function::isIntrinsic) |
| calling conventions | `verifier/cc`, `naked-argument`, `function-type` | `Calling convention parameter requires byval` (`x86_intrcc`), `Calling convention requires void return type` (`spir_kernel`, `amdgpu_kernel`, `amdgpu_cs_chain`), `Calling convention does not allow sret`, `disallows byval` / `preallocated` / `inalloca` / `stack byref` (the shaders and kernels but `spir_kernel`), `Calling convention does not support varargs or perfect forwarding!` (these, `fastcc`, `coldcc`, `ptx_kernel`, `intel_ocl_bicc`), `calling convention does not permit calls` (a call with a kernel's or shader's), `cannot use argument of naked function`, `Function takes metadata but isn't an intrinsic` (token likewise, a returned token), `Function declaration shouldn't have a personality routine`; opt does not compare a call's convention with its callee's |
| call sites | `verifier/attr-*`, `bundle` | the function attributes' rules over the call's (`speculatable attribute may not apply to call sites`, `preallocated as a call site attribute can only be on llvm.call.preallocated.arg`, `Attribute 'sret' cannot be used for vararg call arguments!`, `inalloca isn't on the last argument!`, `immarg operand has non-immediate parameter`, `Return type cannot be token for indirect call!`, `Cannot invoke an intrinsic other than donothing, ...`); operand bundles: `Multiple deopt operand bundles` (`gc-transition`, `funclet`, `cfguardtarget`, `ptrauth`, `kcfi`, `gc-live`), `Expected exactly one funclet bundle operand`, `Funclet bundle operands should correspond to a FuncletPadInst`, `Expected exactly two ptrauth bundle operands`, `Direct call cannot have a ptrauth bundle`, `The 'convergencectrl' bundle requires exactly one token use.` |
| `musttail` | `verifier/musttail`, `parser/musttail` | in opt's order: `cannot use musttail call with inline asm`, `mismatched varargs`, `mismatched return types` (an intrinsic callee exempts the varargs, counts and parameter types), `mismatched calling conv`, `musttail call must precede a ret`, `musttail call result must be returned`, `mismatched parameter counts`, `mismatched parameter types`, `mismatched ABI impacting function attributes` (`sret`, `byval`, `inalloca`, `inreg`, `swiftself`, `swiftasync`, `swifterror`, `preallocated`, `byref` with their types; `nest` and `returned` excluded); `expected '...' at end of argument list for musttail call in varargs function` |
| intrinsics | `verifier/intrinsic-signature` over `arbace.repr.llvm.intr` | the table `intr/signatures` (about 130 intrinsics: memory, overflow and saturating arithmetic, `fshl`/`fshr`, bit counting, `abs`, min/max, the floating-point math ones, `powi`, `is.fpclass`, `lround`, `fptosi.sat`, every vector reduction, `masked.load`/`store`, `ptrmask`, `objectsize`, `prefetch`, `scmp`/`ucmp`, ...) as Intrinsics.td's descriptors, which opt reports in a fixed phrasing: `intrinsic return type (overload type 0) expected any integer or integer vector, but got ptr`, `intrinsic argument 1 type (matching overload type 0) expected i32, but got i64`, `intrinsic return type (vector element of overload type 0) expected i32 (overload type 0 is <4 x i32>), but got i64`, `intrinsic return type (same vector width of overload type 0) ...`, `intrinsic return struct element 1 type expected i1, but got i32`, `intrinsic return type expected literal non-packed struct with 2 elements, but got ptr`; the return and the arguments are checked first, the argument count after them (`intrinsic has incorrect number of args. Expected N, but got M`), the descriptors that name an overload not yet seen at the end; AutoUpgrade's old forms (`memcpy` with the alignment argument, `ctlz` without `i1`, `objectsize` without the dynamic flag, `masked.load` with the alignment, `lifetime` with its size) match their old types; `llvm.ptrmask`'s three call rules; the names `intr/recognized?` takes for intrinsics are the pin's generic table (709 names) and its targets' prefixes |
| funclets | `verifier/funclet`, `eh-pad-entry` | `CatchSwitchInst needs to be in a function with a personality.` (`CatchPadInst`, `CleanupPadInst`, `ResumeInst`), `CleanupPadInst not the first non-PHI instruction in the block.`, `CatchPadInst needs to be directly nested in a CatchSwitchInst.`, `CleanupPadInst has an invalid parent.`, `EH pad cannot be in entry block.`, `EH pad must be jumped to via an unwind edge`, `Block containg CatchPadInst must be jumped to only by its catchswitch.`, `CatchSwitchInst handlers must be catchpads`, `CatchReturnInst needs to be provided a CatchPad`, `CleanupReturnInst needs to be provided a CleanupPad`, `CleanupReturnInst must unwind to an EH block which is not a landingpad.`, `EH pad cannot handle exceptions raised within it`; `expected scope value for catchpad` (parser) |
| metadata | `verifier/md-*` | `Unfinished range!`, `The lower limit must be an integer!`, `Range types must match instruction type!`, `Range must not be empty!`, `Intervals are overlapping` / `contiguous` / `not in order`; `align takes one operand!`, `align metadata value must be an i64!` / `a power of 2!`; `nonnull metadata must be empty`, `noundef applies only to load instructions`, `dereferenceable, dereferenceable_or_null take one operand!`, `invariant.load metadata is only for loads and readonly intrinsic calls`, `invariant.group ...`, `fpmath requires a floating point result!` / `accuracy must have float type` / `not a positive number!`, `scope list must consist of MDNodes`, `scope must have two or three operands`, `noalias.addrspace type must be i32!`, `TBAA metadata cannot have 0 operands`, `Access type node must be a valid scalar type`, `Access bit-width not the same as description bit-width`; `!prof` (`expected string with name of the !prof annotation`, `expected either branch_weights or VP profile name`, `Wrong number of operands` with the origin string `!\"expected\"` skipped, `Wrong number of InvokeInst branch_weights operands`, `!prof branch_weights are not allowed for this instruction`, a function's `first operand should be 'function_entry_count' or ...`); the module flags (`incorrect number of operands in module flag`, `invalid behavior operand ... (expected constant integer)` / `(unexpected constant)`, `invalid ID operand ...`, the `require`, `append`, `max`, `min` values, `wchar_size metadata requires constant integer argument`, `module flag identifiers must be unique (or of 'require' type)`, `invalid requirement on flag, flag is not present in module`) and `llvm.ident` |
| debug info, verdict `debug` | `di/node`, `di/function`, `di/record`, `di/global`, `di/version`, `parser/di-field` | opt prints the message, then `warning: ignoring invalid debug info`, strips it and goes on (exit 0): `subprogram definitions must have a compile unit`, `subprogram declarations must not have a compile unit`, `invalid subroutine type`, `location requires a valid scope` (`DILocation's scope must be a DILocalScope` for an instruction's `!dbg`), `local variable requires a valid scope`, `invalid file`, `invalid type ref`, `invalid scope`, `invalid global variable list`, `invalid retained nodes list`, `invalid composite elements`, `array types must have a base type`, `Subrange can have any one of count or upperBound`, `DWARF address space only applies to pointer or reference types`, `line specified with no file`, `missing global variable type`, `function !dbg attachment must be a subprogram`, `function declaration may only have a unique !dbg attachment`, `!dbg attachment points at wrong subprogram for function`, `mismatched subprogram between #dbg record variable and DILocation`, `invalid #dbg record variable` / `expression`, `!dbg attachment of global variable must be a DIGlobalVariableExpression`; a Debug Info Version other than 3 (or none) with debug info: `ignoring debug info with an invalid version (N)`; only the nodes reachable from the named metadata, the attachments and the records are checked, as opt visits them. LLParser's own: `missing required field 'X'` (the table `di-required`, derived from opt by dropping each field of each node kind), `missing 'distinct', required for !DISubprogram that is a Definition`, `'checksumkind' and 'checksum' must be provided together`, `invalid debug location` |

The reader takes function attributes in a parameter's or a return value's place (the grammar of LLParser's
`parseOptionalParamAttrs` accepts any attribute word and rejects it by Attributes.td), so the placement rules see them.

**The check**, `bin/repr-llvm verify-check LIST WORK`: opt (`REPR_LLVM_OPT`, default `/usr/lib/llvm23/bin/opt -passes=verify
-disable-output`) and the verifier on each file of LIST, `WORK/verify.tsv` (NAME, opt's verdict, Arbace's, the verdicts
same, the first messages same, the two messages); opt's message is its first line that is not a warning, without its
program and position (an abort's `LLVM ERROR` after the message aside). Exit 1 on a different verdict. Its invalid inputs
are mutants, `bin/repr-llvm mutants LIST OUT [N [FIRST]]` (`arbace.repr.llvm.mutate`, deterministic by the mutant's
index): one change of a valid module's forms, printed: an operand's type changed (`i32` ↔ `i64`, `ptr` → `i64`, a branch
condition widened), a use moved above its definition, a terminator removed, a phi's incoming block renamed (to a block that
does not exist or to another of the function's), a phi moved below a non-phi, a phi without one entry, an alignment
not a power of two, a declaration's linkage or a global's visibility made illegal, an operand renamed to an undefined value
or to the instruction's own result, a result renamed to an earlier one's, a landingpad moved down, an intrinsic call
without its last argument, a branch to the entry block; the moves also with the function renumbered (`renumber`), so that
the parser's numbering holds and the Verifier decides. On 2026-11-04: the 282 modules of §6.1 (all valid: 0 errors from
both) and 630 mutants (two sets of 315, the second, indices 1,000 on, written after the rules were fitted to the first):
**912 of 912 verdicts as opt's, 613 of 613 rejections with opt's first message** (17 mutants opt takes, a type change of
a load's result unused, a hoist that keeps dominance: valid here too); the rules' share of the 613: numbering 103,
dominance 78, undefined values 67, phi predecessors 40, alignment 40, intrinsic calls 38, entry predecessors 38, blocks
without terminators 36, value types 31, linkage 27, forward types 23, phis not grouped 19, self-references 17, unwind
destinations 15, phi entries 13, visibility 13, the rest 15. Seventy-three hand-written probes, one a rule each, written
while porting: 72 of 73 verdicts and 64 of 65 messages (the twice-defined label). About 45 s for the 912, opt's runs serial.

**The check, extended (LL4b).** `verify-check` has a third verdict, `debug`: opt exits 0 on a module whose debug info is
broken after printing the message (its first line is the message, `warning: ignoring invalid debug info` follows) and a
`Debug Info Version` it does not know prints only the warning (`ignoring debug info with an invalid version (N)`); the
verdicts are compared as `valid`, `invalid` and `debug`, and `bin/repr-llvm verify` prints a `debug` file's message as a
`warning`, exit 0. `bin/repr-llvm mutants LIST OUT [N [FIRST [FAMILY]]]` takes a FAMILY of kinds (`arbace.repr.llvm.mutate/
families`): `base` (the 17 kinds above), `attrs` (a pointer-only attribute on a non-pointer and the reverse, mutually
exclusive pairs on a parameter, an argument or the function, an attribute where Attributes.td does not allow it, a
once-only attribute twice, `returned` of another type, invalid attribute values, `immarg` on a non-intrinsic), `cc` (a
convention that restricts the function it names; a call with a kernel's), `musttail`, `bundles`, `intrinsics` (a
declaration's type changed, an argument `undef`, a name replaced by another intrinsic's), `funclets` (a pad, a parent, a
position, a handler, a bundle token or the personality changed; an invoke's unwind edge to its normal destination),
`metadata` (an attachment the Verifier checks with a node that breaks its rules; an operand of a `!range`, `!prof`, `!tbaa`,
module flag or `llvm.ident` node dropped, added or replaced), `debug` (a field of a DI node dropped, a reference sent to a
node of another kind, a record's variable, expression or location replaced, a `!dbg` attachment sent elsewhere, the
`Debug Info Version` changed). Without a family every kind cycles (41 kinds). A mutant of the module's named types is not
made (`%name` is also a type's spelling).

On 2026-11-04, on the 282 modules of the lists above (all valid: **282 of 282 verdicts as opt's, 0 invalid**; one, `misc`, has `!dbg`
attachments and no `Debug Info Version`, `debug` from both) the sets of each family (indices from 0; the rules were fitted to
them), opt's verdicts and first messages:

| set | mutants | verdicts as opt's | first messages as opt's |
|---|---|---|---|
| attrs | 200 | 200 of 200 | 168 of 168 |
| cc | 100 | 100 of 100 | 74 of 74 |
| musttail | 100 | 100 of 100 | 95 of 95 |
| bundles | 100 | 100 of 100 | 88 of 88 |
| intrinsics | 150 | 150 of 150 | 120 of 130 |
| funclets (16 modules with EH) | 150 | 148 of 150 | 141 of 144 |
| metadata | 97 | 97 of 97 | 91 of 91 |
| debug | 200 | 197 of 200 | 160 of 164 |
| base, indices 0 on (the 278 modules of §6.1) | 294 | 294 of 294 | 284 of 284 |
| base, indices 1,000 on | 294 | 294 of 294 | 285 of 287 |

(the earlier 630 mutants, `bin/repr-llvm verify-check` over the 912 files of 2026-11-04: 912 of 912 verdicts, 614 of 614 first
messages with these rules). The held-out sets, generated after the rules were fixed, all kinds together (indices 100,000 and
200,000 on, 600 asked, over the 282 modules and eight hand-written funclet modules): the first, 542 mutants: **539 of 542
verdicts as opt's, 470 of 481 first messages** as generated, before the three fixes it showed (`immarg` of a call's argument that
the callee's parameter lacks, a DI node's `baseType` of the wrong kind, a lexical block's scope: not a Verifier rule), and 541 of
542, 474 of 481 with the final rules; the second, 544 mutants: **534 of 544 verdicts, 479 of 486 first messages** as generated, before its
fixes (the old intrinsic forms' calls and users, `immarg` positions from Intrinsics.td, the attributes of a vararg argument), and
541 of 544, 480 of 486 with the final rules. What the held-out sets still show: `is.fpclass`'s constant mask, `eh.exceptionpointer`'s argument, a
DI `type` of a global variable expression, the sibling funclet rules, an intrinsic renamed to one whose arguments count
differs (opt's order of the count and the types, and its abort).

The hand-written modules are committed: `test/llvm/hw/` (the four modules with comments, blank lines, odd spacing, numbered
and unnamed values, unlabelled blocks, hex floats and laid-out metadata; `test/llvm/hw.list`, NAME<TAB>PATH from the
repository's root; the round trips of §6.1 and `verify-check` take it) and `test/llvm/probes/` (478 small modules, one
rule each, by family: `verify/` the 73 of LL4, `attrs/`, `cc/`, `musttail/`, `funclets/`, `metadata/`, `debug/` (from
`hw/dbg.ll`), `intrinsics/` (`good.ll`: a valid declaration of every intrinsic of the table); `test/llvm/probes.list`):
`bin/repr-llvm verify-check test/llvm/probes.list WORK` gave opt's verdicts on 467 of 478, first messages 310 of 323 (the others: syntax outside the
reader's grammar, the twice-defined label, swifterror's call rules, `clang.arc.attachedcall`, VFABI names, `PIC Level`, a
funclet's parent of itself); §6.2.2 ported those rules and compares the whole of opt's stderr: verdicts 472 of 478, first messages 319 of 323,
whole stderr 470 of 478, the eight others and the two modules the reader refuses classed with their reasons in `test/llvm/probes-known.tsv`.

### 6.2.2 opt's whole stderr (LL4c)

The rules of §6.2 and §6.2.1 gave opt's verdict and its first message. Since 2026-11-04 (branch `llvm-verify3`)
`bin/repr-llvm verify FILE.ll` prints what `opt -passes=verify -disable-output FILE.ll` prints to stderr, line for line,
and `verify-check` compares the whole of it (`WORK/full.tsv`; opt's in `WORK/opt/NAME.log`, verify's in
`WORK/arb/NAME.err`; the program's name as opt's, `REPR_LLVM_OPT`). What opt prints, and where it comes from here:

```
$ bin/repr-llvm verify test/llvm/probes/verify/addf.ll test/llvm/probes/funclets/v8.ll
/usr/lib/llvm23/bin/opt: test/llvm/probes/verify/addf.ll:2:12: error: invalid operand type for instruction
  %x = add float %c, %c
           ^
A single unwind edge may only enter one EH pad
  invoke void @g()
          to label %ok unwind label %cp
FuncletPadInst must not be nested within itself
  %p = cleanuppad within %p []
/usr/lib/llvm23/bin/opt: test/llvm/probes/funclets/v8.ll: error: input module is broken!
```

- **A parser error** (LLParser's, the first one, which stops opt): `PROG: FILE:LINE:COL: error: MESSAGE`, the source line
  and a caret line (SMDiagnostic). The position comes from the reader's side table: `arbace.repr.llvm.read/read-module-items`
  gives each top-level form's items (a definition's header, label, instruction, debug record, `}`) with their offsets in the
  text, the verifier's context says which item a check runs at, and the check's **hint** says which token of the item
  LLParser reports it at (`arbace.repr.llvm.diag`: `[:ty]` an instruction's type, `[:op N]`, `[:call-arg N]`, `[:param N]`,
  `[:find W]`, `[:attr-val NAME]` the value of an attribute, `[:comma N]`, `[:next]` the next top-level form, ...; the
  names of undefined values are searched for in their function or the module: `[:fn-find W]`, `[:mod-find W]`). A `.ae` text
  has no source, its error has no position (`PROG: FILE: error: MESSAGE`). The hints were found by probing opt; each rule
  of §6.2's parser table has one.
- **The Verifier's messages**, all of them: the Verifier goes on after a failed check, each visit ending at its first failure
  (visitFunction, a block's phis, each instruction, a module flag, ...; `visiting` in `verify.ae`), so a module broken in
  two places prints two messages; some checks print more than one message from one visit (an invoke of an intrinsic twice,
  a `branch_weights` where none is allowed and then its count, every operand of a dominance check, `CheckFailed` against
  `Check`). After the message, **the values it names**, one a line (the `culprits` of `verr`, `:inst` by default for an
  instruction's check, `ptr @f` for a function's, `label %bb`, `:none` for those that print nothing): an instruction in the
  AsmWriter's form, a function as `ptr @f` or whole (`declare ...`, for an intrinsic's signature), a metadata node
  (`!N = ...`), an argument (`ptr %x`), a constant (`i32 0`). Then `PROG: FILE: error: input module is broken!`, or, for a
  module with `!llvm.dbg.cu` (every such module, observed: opt ends in the pass's `report_fatal_error`),
  `LLVM ERROR: Broken module found, compilation aborted!`, the bug report line and the first two lines of the stack dump
  (`Stack dump:`, `0.	Program arguments: ...`). The debug info's messages end with `warning: ignoring invalid debug info in FILE`,
  an unknown `Debug Info Version` with `warning: ignoring debug info with an invalid version (N) in FILE` before the
  others.
- **The AsmWriter's forms** of what is printed, by the printer of §6 (which prints what clang printed, so most of it holds)
  and these, each found from opt's output and checked against the 282 modules' own text:
  a load, store or alloca written without `align` prints its type's ABI alignment from the module's data layout
  (`dl-specs`, LLVM's defaults and the `target datalayout` string's `i`, `f`, `v`, `p` entries); a floating constant prints as
  `%e` when that reads back to the same value, else as the double's bits (`canon-fp`); typed pointers as `ptr`; the attributes
  of a parameter, a return value or a declaration in Attribute's order (enum attributes by their enum's name, type
  attributes, integer ones, ranges, strings: `sort-attrs`), a declaration with its `; Function Attrs:` line; **the numbers**
  the slot tracker gives: a function attribute set is `#N` by first use, the functions' sets in module order, then the sets of
  the calls **of the function printed** (`group-numbers`; `group-mismatches` finds none in 281 of the 282 modules, the
  hand-written one apart), and a metadata node is `!N` by the traversal (`arbace.repr.llvm.diag/md-slots`, a node before its
  operands, in the operand order of each DI kind: the globals' attachments, named metadata, then each function's attachments,
  the debug records' value, variable and location, a call's metadata arguments and the attachments: `md-slots` renumbers none
  of the 281 modules' nodes, so a mutant whose instructions moved prints the numbers opt does; `slots-check`, `groups-check`
  of `bin/repr-llvm`'s namespace).
- **Order**: a block's predecessors and a phi's entries are sorted by the order the parser creates the blocks in (a block
  when first mentioned, `bb-ranks`; LLVM sorts them by pointer).

The rules ported since §6.2.1: the funclet nesting (`A single unwind edge may only enter one EH pad`, `EH pad jumps through a
cycle of pads`, `A cleanupret must exit its cleanup`, `FuncletPadInst must not be nested within itself`, `Unwind edges out of a
funclet pad must have the same unwind dest`) and the sibling rule (`EH pads can't handle each other's exceptions`,
`check-sibling-unwinds`); swifterror arguments of a call (`swifterror argument for call has mismatched alloca` and `... parameter`,
`... should come from an alloca or parameter`); `clang.arc.attachedcall` (the return type, one function as its argument, one bundle);
VFABI names (`invalid name for a VFABI variant`, the grammar `_ZGV<isa><mask><vlen><params>_<name>[(<vector name>)]`, one parameter
for each of the function's); `llvm.is.fpclass`'s mask (`unsupported bits for llvm.is.fpclass test mask`), `eh.exceptionpointer`'s
argument; the `PIC Level` module flag (an `error` flag of `PIC Level`, `sign-return-address{,-all,-with-bkey}` and
`branch-target-enforcement` is a `min` one, `PIE Level` a `max` one: AutoUpgrade), `SemanticInterposition`; `DbgLoc uses atomGroup
but DISubprogram doesn't have Key Instructions enabled`, `scope points into the type hierarchy`, the parser's positions of the DI
fields' errors; and the culprits and the second messages of the rules of §6.2.1 (a failed check of a call's attributes no longer
ends the call's visit, a failed parameter's attributes no longer end the function's).

**The check, whole stderr.** On 2026-11-04, the sets of §6.2.1 regenerated (indices from 0, the same mutants, the rules had been
fitted to their first messages, not to the rest of the output), `verify-check`, **whole stderr as opt's**, with the verdicts and
first messages beside it (the table of §6.2.1 had them; they hold, but for the first messages of `base`, 1 and 3 fewer, and
the verdicts of the funclets, 150 of 150, and of the debug info, 198 of 200, which are more):

| set | mutants | whole stderr as opt's (and verdicts, first messages) |
|---|---|---|
| attrs | 200 | 198 of 200 (verdicts 200 of 200, first messages 168 of 168) |
| cc | 100 | 99 of 100 (verdicts 100 of 100, first messages 74 of 74) |
| musttail | 100 | 98 of 100 (verdicts 100 of 100, first messages 95 of 95) |
| bundles | 100 | 95 of 100 (verdicts 100 of 100, first messages 88 of 88) |
| intrinsics | 150 | 138 of 150 (verdicts 150 of 150, first messages 120 of 130) |
| funclets (16 modules with EH) | 150 | 148 of 150 (verdicts 150 of 150, first messages 144 of 144) |
| metadata | 97 | 95 of 97 (verdicts 97 of 97, first messages 91 of 91) |
| debug | 200 | 188 of 200 (verdicts 198 of 200, first messages 160 of 164) |
| base, indices 0 on (the 278 modules of §6.1) | 294 | 287 of 294 (verdicts 294 of 294, first messages 283 of 284) |
| base, indices 1,000 on | 294 | 289 of 294 (verdicts 294 of 294, first messages 284 of 287) |

and the held-out sets of this round, generated after the rules were fixed and run once, at the end (all kinds together, indices
500,000 and 600,000 on, 600 asked of the 282 modules and the eight hand-written funclet modules, as §6.2.1's): the first, 543 mutants: **541 of 543 verdicts, 522 of 543 whole stderr** (480 of 488 first messages); the second, 539 mutants: **538 of 539 verdicts, 518 of 539 whole stderr** (479 of 488 first messages): 1,040 of 1,082 whole stderr, 96.1 %. Of the 42 that differ, by kind of mutant: 9 `intr-rename`, 8 `phiblock`, 6 `di-retarget`, 3 each `md-attach`, `intr-decl`, `attr-place`, 2 each `musttail`, `di-attach`, `attr-type`, 1 each `intr-arg`, `bundle`, `attr-pair`, `attr-dup`.
(Two sets of indices 300,000 and 400,000 served while the rules were written: 526 of 543 and 524 of 544 at the end of that work,
they found a hang of the funclet checks, a pad whose parent chain loops without returning to it, fixed.)
The 282 valid modules: **282 of 282 whole stderr as opt's** (empty but `misc`'s warning), the four hand-written ones 4 of 4.
The probes: **whole stderr 470 of 478, verdicts 472 of 478, first messages 319 of 323**. The 8 that differ and the two valid modules the reader refuses are in `test/llvm/probes-known.tsv`
(`REPR_LLVM_KNOWN`, `NAME<tab>CLASS<tab>reason`; `verify-check` counts them apart): a label defined twice (`a:` ... `a:`,
LLParser appends the second block's instructions to the first's), syntax the reader takes and LLParser rejects (`initializes()`,
`alloc_size` without its argument, `memory(bogus: read)`), a metadata operand opt prints by its address (`metadata <0x7f...>`), a
node written inline in a module flag (the slot tracker gives it a number and moves the rest) and equal nodes LLParser merges.

What still differs in the sets (by reason; each a few mutants):
- the attributes and group of an intrinsic's declaration: opt gives a declaration whose name is an intrinsic the intrinsic's own
  attributes and group (Intrinsics.td), not the text's, so its `; Function Attrs:` line and `#N` differ after a rename or a
  mismatching declaration; an intrinsic renamed so that its signature mismatches is rewritten by AutoUpgrade, opt prints the
  upgraded declaration (`declare ptr @llvm.ctlz.p0(ptr, i1)`) or aborts;
- the pointer order of blocks: opt sorts a block's predecessors by address, here by the order of their first mention, which
  differs for blocks referenced before they are defined (`PHI node entries do not match predecessors!`, the labels printed);
  the order of two incoming values of one block is the sort's, not stable here;
- metadata numbers of nodes written inline, of the uniqued nodes, and of a call whose `!dbg` opt numbers as the first node of a
  later function (a `convergencectrl` bundle: the printed instruction's slot tracker is not the module's);
- debug info rules not ported: a node reached twice printed twice, `invalid tag`, `DILocation not allowed within this metadata
  node`, `invalid retained nodes, retained node is not local`, the global variable list's elements;
- `immarg`/swifterror rules' second messages, `Attribute writable and memory without argmem: write are incompatible!`;
- two messages from one `CheckFailed` of `vscale_range` and attribute pairs.

### 6.2.3 llvm/test's rejection tests (LL4, the corpus loop, 2026-11-05)

Branch `llvm-ll4`. The work list was llvm/test's rejection tests (`test/llvm/llvm-test-reject*.list`, doc/llvm/LLVM-CROP-SURVEY.md,
"As built: LL1" and "As built: LL4"), each failure taken to the rule of `Verifier.cpp` or `LLParser.cpp` (or the files they call)
that decides it, ported, and the whole of opt's stderr compared. What changed in the verifier's machinery:

- **LLParser's syntax errors, in the reader.** `arbace.repr.llvm.read` throws `llerr`, an error with LLParser's message and the
  offset of the token LLParser reports it at, where the grammar is LLParser's: `expected 'X'` of every punctuation `expect!`,
  `expected top-level entity` (also for `target` and `source_filename` after the header, LLParser::parseTargetDefinitions),
  `expected instruction opcode`, `found end of file when expecting more instructions`, `expected type`, `expected '('` of the
  type attributes, `expected integer` / `expected ')'` of `align(N)` and `prefalign(N)`, `expected metadata operand`,
  `expected '{' here`, `expected attribute group id`, `expected metadata after comma`, `expected debug record type here`,
  `expected '!' here` of a record's metadata operands, `expected uselistorder directive`, the musttail ellipsis's two errors,
  `expected uint32 param`, the target types' parameter counts (Type.cpp's `checkParams`), the removed constant expressions
  (`extractvalue constexprs are no longer supported` and the 28 others), the summary entries' heads, `unknown property name` of
  `module asm(...)`; and LLLexer's: `unterminated comment`, `NUL character is not allowed in names`, `hexadecimal constant too
  large for half (16-bit)` / `bfloat`, `bitwidth for integer or byte type out of range`, `invalid address space, must be a 24-bit
  integer`, `ptr* is invalid - use ptr instead`, `invalid array element type`, `invalid vector element type`. `verify-file`
  prints it as SMDiagnostic does (tabs of the source line expanded to eight columns, `diag/parse-error-text`).
- **The checks before the error.** The reader reads in partial mode: at its error (an `llerr` or any refusal with an offset) it
  stops with the forms read so far, a function cut at the failing instruction; LLParser's checks run on them without the
  end-of-function and end-of-module ones (`*partial*`), and an error of theirs at an earlier offset is opt's. A refusal of the
  reader's own (outside the grammar here) stays `unread` unless an earlier error decides.
- **The specialized nodes' fields** (`arbace.repr.llvm.di-table`, generated by `bin/llvm-corpus di-table` from `LLParser.cpp`'s
  `VISIT_MD_FIELDS` of every `parseDI*`/`parseGenericDINode`, `Dwarf.def`, `Dwarf.h` and `DebugInfoFlags.def`): each field's
  value checked at its token as `parseMDField` does for its type (unsigned and signed limits, `LineField`, `ColumnField`,
  the DWARF keywords by prefix and name: tags, languages, encodings, virtualities, calling conventions, source language
  names and dialects, enum kinds, macinfo types; emission and name table kinds; DIFlag and DISPFlag sets; `true`/`false`;
  `null` where not allowed; empty strings), `invalid field 'X'`, `field 'X' cannot be specified more than once`, the
  required fields at the closing parenthesis, the compile unit's language fields; DIExpression's elements (`invalid DWARF op`,
  `element too large`).
- **The intrinsics' table** (`arbace.repr.llvm.intr-table`, generated by `bin/llvm-corpus intrinsics`: the pin's `llvm-tblgen
  --dump-json` over `Intrinsics.td`, 16,423 intrinsics with their IIT type signature, overloading, `ImmArg`, `Range` and
  `IntrNoMem` properties) with `Intrinsics.cpp`'s decoding, `lookupIntrinsicID`, `matchIntrinsicType` (every descriptor kind and
  message) and `getMangledTypeStr` ported in `arbace.repr.llvm.intr`: the signatures of every intrinsic, the immarg positions and
  ranges (`immarg value V for arg I out of range [L,H)`), LLParser's implicit declarations (`unknown intrinsic`, a call's
  signature reported at the name's first reference, the declarations named by their mangled overloads in printed calls, each
  call by its own type), AutoUpgrade's masked intrinsics with their alignment (`LLVM ERROR: Invalid alignment argument`) and its
  deleted `lifetime.*.i64`; `intr/recognized?` is the table's lookup.
- **Inline metadata** is numbered past the parser as the slot tracker does (`hoist-inline-md`: tuples and specialized nodes but
  DIExpression and DIArgList, structurally equal non-distinct nodes one node, inline or not), so the Verifier's culprits print
  `!captures !0` and `!0 = !{}`; nodes print as the AsmWriter writes them (`canon-di`: its field order from `AsmWriter.cpp`, the
  defaults it skips, `spFlags` always, from the legacy fields when absent, flags in DebugInfoFlags.def's order; a node referring to
  itself `distinct`; floats canonical; an invalid DIExpression by its numbers).
- **Debug info and opt's two verifications.** A Debug Info Version other than 3: `StripDebugInfo` (the version warning only when
  it changes something) and the rest verified with the debug info's errors hard (what is still reachable: named metadata but
  `llvm.dbg.*` and `llvm.gcov`, the attachments but `!dbg`). Version 3 with soft debug info errors: the warning, the strip, and
  opt's second verification of what is left (an error there makes the module invalid).
- **`CheckFailed` against `Check`**: `soft!` records a failure and goes on (branch protection values, unsigned attributes,
  `allockind`'s modifiers, target-features' empty entries, StrBoolAttr values); a `visitXMetadata` of `visitInstruction` ends only
  itself (`sub`), its own checks the instruction's visit.

The rules added (by Verifier visitor): visitGlobalVariable and visitGlobalValue whole (initializers sized, common globals,
`llvm.global_ctors`/`dtors`, `llvm.used`/`compiler.used`, the address space's size, declarations' linkage, `associated`,
`implicit.ref`, `elf_section_properties`, `absolute_symbol`, appending linkage, comdats of declarations, DLL storage and
visibility, scalable globals, ptrauth constants' deactivation symbols), visitGlobalAlias with visitAliaseeSubExpr,
visitGlobalIFunc, visitFunction's metadata (`verifyFunctionMetadata`, single `!prof` and `!kcfi_type`), token-like and
`x86_amx` function types (Type.cpp's target type properties), verifyFunctionAttrs's later rules (`sanitize_realtime`, the SME
states, `writable` with `memory`, `target-features`, `alloc-variant-zeroed`, `denormal_fpenv` on calls, functions' attributes
that are not function attributes, `huge 'byval' arguments`), visitLoadInst and visitStoreInst (sized, atomic orderings, types
and sizes, sync scopes), visitInstruction's attachments in its order (`nofpclass`, `nofree`, access groups, `callee_type`,
`mmra`, `annotation`, `captures`, `inline_history`, `mem.cache_hint`, `amdgpu.expected.active.lanes`; verifyRangeLikeMetadata
whole, with wrapping intervals), visitIntrinsicCall's `assume` bundles, `gcroot`/`gcread`/`gcwrite`, `gc.statepoint`'s GC and
callee type, `localescape`/`localrecover`, `va_start`, `threadlocal.address`, `reloc.none`, `bswap`, `ucmp`/`scmp`,
`memset.pattern`, `fptrunc.round`, the constrained intrinsics' exception and rounding arguments, `get.vector.length`,
`structured.alloca`, `nvvm.setmaxnreg`, the exclusive load/store intrinsics' `elementtype`, `aarch64.prefetch` and
`range.prefetch`, `sponentry`, `ptrmask` in its order; verifyNoAliasScopeDecl, verifyInlineAsmCall, verifyTailCCMustTailAttrs,
VerifyTypeAlign, inalloca arguments' allocas, intrinsics whose address a global takes, the TBAA tags after UpgradeTBAANode,
`llvm.errno.tbaa`, the CG Profile module flag, the `Linker Options` flag; the DI type nodes' visitors (visitDIDerivedType,
visitDICompositeType, visitDISubrange, visitDIGenericSubrange, visitDISubroutineType with visitDIType), DIExpression::isValid,
`!DIAssignID`'s placement; and LLParser's: struct and undef/poison/zero constants' types, constant expressions' types and casts,
`getelementptr`'s indices (getIndexedType) and vector widths, `cmpxchg`/`atomicrmw` orderings and operands, `alloca`'s and
`landingpad` clauses' types, function-local names in metadata tuples and initializers, `select`/`phi`/call fast-math flags,
vector selects, use-list orders (indexes and uses), comdats (undefined, unnamed, redefined), forward references of functions
and globals in another address space, recursive struct bodies (StructType::checkBody), `Metadata id is already used`, invalid
`!dbg` and debug record locations, mixed debug formats, `dso_local dllimport`, the data layout's address spaces, empty
`range`/`allockind`/`uwtable` values, `allocsize`'s same index, huge parameter alignments, musttail calls that forward their
varargs (the reader's `:ellipsis`, printed back as `...`).

Not ported (by class; the records name them): the AMDGPU and other target-specific intrinsic rules of visitIntrinsicCall (9
tests of Verifier/AMDGPU), the vector-predication, vector insert/extract, matrix, statepoint relocation and convergence
intrinsics' rules, `DILocation not allowed within this metadata node` (visitMDNode's AreDebugLocsAllowed), memprof and
modular-format, `Missing funclet token on intrinsic call`, the logical-pointer and structured-gep modules, `dso_local_equivalent`,
a few lexer cases (`*/` at the top, `u0x` integers, inline asm constraint syntax), and the order of opt's output where it
depends on the use lists (two users of one intrinsic).
