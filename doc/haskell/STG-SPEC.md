# STG forms: GHC's final STG written in Clojure

Status: milestone HE1 of Haskell's eval ([HASKELL-EVAL-SURVEY.md](HASKELL-EVAL-SURVEY.md) §4, §11; HES1-HES21 decided
by the user on 2026-11-03), written 2026-11-03 with HE2's helper, which writes these forms, and its checker, which
reads them back: the grammar below is the one `arbace.repr.haskell.stg` checks over every dump (`bin/hsi dumps`), and
§10's checklist is the one `bin/hsi spec-check` compares with GHC 9.14.1's sources. Choices made while writing it are
STG-S1-STG-S12 (§12).

**STG forms** are GHC's final STG of one module, the `[CgStgTopBinding]` GHC's code generator receives (after
CorePrep, CoreToStg, the STG simplifier, unarisation and tag inference, each closure with its free variables), with the
module's data constructors, written as data for Arbace's reader. They are the input of hsi and of Haskell's AOT (HES3
(a), HES15 (a)): a compiler's IR, written by the helper (`tools/hsdump stg`, `tools/hsdump/Stg.hs`), never printed
back as Haskell (HES20 (a)). They are a second representation beside the Haskell forms of [SPEC.md](SPEC.md), which
stay the read and print levels' representation.

Words: **the helper** is `tools/hsdump` (repr/haskell's, HES4 (a)); **GHC** the host's GHC 9.14.1
(`tools/hsdump/GHC_VERSION`); **`GHC.Stg.Syntax`** that module of GHC 9.14.1's sources (the pinned
`ghc-9.14.1-src.tar.xz`, `compiler/GHC/Stg/Syntax.hs`).

Contents: 1 Principles · 2 The module · 3 Names · 4 Representations · 5 Literals · 6 Bindings and right-hand sides ·
7 Expressions · 8 Data constructors · 9 Core forms · 10 The checklist · 11 What the forms leave out · 12 Choices ·
Appendix A: a worked example.

## 1. Principles

- One file per module, `MODULE.stg.ae`, a sequence of top-level forms read by Arbace's reader (the Clojure reader's
  syntax: lists, vectors, maps, strings, keywords, integers of any size, `0x` hexadecimal). Nothing is evaluated.
- Every node is a list `(head field...)` whose head is an unqualified symbol, but the module's two directive heads
  `stg/module` and `stg/data`. The heads are few (§10) and each is known at its place: the checker rejects an
  unknown head, a known head in the wrong place, or a field of the wrong kind.
- **Names are strings** (§3), never symbols: Haskell's names hold characters the reader cannot take in a symbol
  (`\\`, `/=`, `@`, `~`, `#` first, `:` first). A string in an atom's place is a variable; literals are lists (§5).
- **Representations, not types** (§4): every binder carries its `PrimRep`s, the only part of its type the machine
  needs; types are left out (§11).
- **Deterministic**: two compilations of the same sources with the same flags write the same bytes (`bin/hsi dumps
  --again`). GHC's uniques are not deterministic across runs (they depend on the order of work under `-j`), so
  internal names are numbered by the helper in the order of the dump (§3).
- **Positional fields**, an optional trailing map for the rare ones (`{:number n}`, `{:tycon ...}`, a foreign
  target's `{:unit ... :fun false}`), as Haskell forms and Go forms do.

## 2. The module

```
(stg/module {:unit "UNIT" :module "M" :file "PATH" :ghc "9.14.1" :helper N :helper-hash "sha256:..." :flags [...]})
(stg/data TYCON [(dc ...) ...])          ; one per algebraic type constructor of the module (§8)
(string BINDER "BYTES")                  ; StgTopStringLit
(bind BINDER RHS)                        ; StgTopLifted (StgNonRec ...)
(rec (bind BINDER RHS) ...)              ; StgTopLifted (StgRec ...)
```

- The header is first: the unit id the module was compiled in (`main` for a program, a library's installed id,
  `ghc-internal` for the wired-in unit), its module name, its source file as GHC was given it (relative to the
  directory GHC ran in: `libraries/containers/containers/src/Data/Map/Internal.hs`, so that `HasCallStack` text is
  the installed library's, HES6), GHC's version, the helper's version and hash, and the GHC flags that decide the STG
  (`-outputdir` and `-j` left out).
- Then the `stg/data` forms in GHC's order of the module's type constructors, then the top-level bindings in GHC's
  order (the order the code generator receives them; not sorted: a module's initialization does not depend on it,
  but byte-equality of two runs does).
- A module that defines `main` in `Main` also has the binding of `:Main.main` (`"main::Main/main"`), GHC's
  `runMainIO main` wrapper, the program's entry.

## 3. Names

- **External names** (every top-level binder GHC exports or that tidying externalised, every data constructor, every
  type constructor, every name of another module): `"UNIT:MODULE/OCC"`, e.g. `"ghc-internal:GHC.Internal.Base/map"`,
  `"containers-0.8-b42e:Data.Map.Internal/!"`, `"main:Main/main"`. The unit id has no `:`, a module name no `/`, so a
  name splits at its first `:` and the first `/` after it; the occurrence name is the rest, with any characters
  (`"ghc-internal:GHC.Internal.Base/."`, `"ghc-internal:GHC.Internal.List/\\\\"`). The program's root module `:Main`
  gives `"main::Main/main"`.
- **Internal names** (local binders; top-level binders GHC left internal, such as string literals' `$trModule3`):
  `"OCC_N"`, N a decimal numbered per module by the helper: first every top-level binder in the dump's order, then
  every other name at its first occurrence in the dump's order (a binder before its uses). Two binders GHC gave one
  unique get one number (CorePrep's data constructor workers share their parameters' uniques: the same `"eta_41"`
  in two bindings), which is harmless: binders are scoped. An internal name never contains `:` before a `/` of an
  external's shape, since a local occurrence name starts with a lower-case letter, `_` or an operator character
  other than `:`.
- **Data constructor workers**: a constructor and its worker function have the same external name; which one is meant
  follows from the place (a `con`'s or `rcon`'s first field, an alternative's constructor, a `dc`: the constructor;
  an atom or an `app`'s function: the worker). The worker's binding is in the dump (CorePrep adds it: `(bind
  ["main:Main/Circle" :L] (closure :r [] [["eta_41" :L]] (con "main:Main/Circle" ["eta_41"])))`); a `dc` names
  its worker only when the names differ (§8).
- **Wired-in names** are external names of their defining module (`"ghc-internal:GHC.Internal.Types/:"`, the
  unboxed tuples' constructors `"ghc-internal:GHC.Internal.Prim/(#,#)"` in a `con` after unarisation).

## 4. Representations

A `PrimRep` is a keyword: `:L` (`BoxedRep (Just Lifted)`), `:U` (`BoxedRep (Just Unlifted)`), `:B` (`BoxedRep
Nothing`, levity-polymorphic), `:int` `:int8` `:int16` `:int32` `:int64`, `:word` `:word8` `:word16` `:word32`
`:word64`, `:addr`, `:float`, `:double`; a vector `[:vec N ELEM]` for `VecRep N ELEM`, ELEM one of `:int8` ...
`:word64`, `:float`, `:double` (`PrimElemRep`).

A binder's or a type's representations (`typePrimRep`, a list) are written REPS: one `PrimRep` as itself, none as
`:v` (void: `State# s`, a zero-width unboxed tuple), several as a vector of them (`[:int :L]`; after unarisation
only an unboxed tuple's or sum's own type has several). A representation-polymorphic type (a type of kind `TYPE r`, r
a variable, where GHC's `typePrimRep` has no answer: a field of a representation-polymorphic data constructor, a
binder GHC never allocates) is `:rp` (STG-S13).

A **binder** is `[NAME REPS]`: `["x_12" :L]`, `["ww_3" :int]`, `["s_7" :v]`.

## 5. Literals

| `Literal` | form |
|---|---|
| `LitChar c` | `(char CODEPOINT)` |
| `LitNumber LitNumInt n` ... | `(int n)` `(int8 n)` `(int16 n)` `(int32 n)` `(int64 n)` `(word n)` `(word8 n)` `(word16 n)` `(word32 n)` `(word64 n)` `(bignat n)` (n any size; `LitNumBigNat` only before CorePrep, kept for completeness) |
| `LitString bs` | `(str "BYTES")`: one character per byte, U+0000 to U+00FF (bytes 0x20-0x7E as themselves but `"` and `\\`, `\n` as `\n`, the others as `\uXXXX`) |
| `LitNullAddr` | `(nulladdr)` |
| `LitRubbish tc rr` | `(rubbish :type REPS)` or `:constraint`; REPS from the `RuntimeRep` (`runtimeRepPrimRep_maybe`), else the type as GHC prints it, a string |
| `LitFloat r` | `(float 0xBITS)`: the IEEE 754 binary32 bits of `fromRational r`, as GHC's code generator emits it |
| `LitDouble r` | `(double 0xBITS)`: the binary64 bits |
| `LitLabel fs fod` | `(label "SYMBOL" :fun)` or `:data` (`foreign import ccall "&sym"`) |

A literal is an atom (§7) and an expression (`StgLit`), and an alternative's constructor (`LitAlt`).

## 6. Bindings and right-hand sides

```
(bind BINDER RHS)                                        ; StgNonRec
(rec (bind BINDER RHS) ...)                              ; StgRec, one or more
(closure UPD [FREE...] [BINDER...] EXPR)                 ; StgRhsClosure
(rcon DATACON [ATOM...] {:number N}? [TICK...]?)         ; StgRhsCon
```

- `UPD` is the `UpdateFlag`: `:r` `ReEntrant` (a function, or a non-updatable closure), `:u` `Updatable` (a thunk),
  `:s` `SingleEntry`, `:j` `JumpedTo` (a join point's closure; GHC 9.14 writes it for let-no-escape bindings it
  compiles as jumps).
- `[FREE...]`: the closure's free variables (the `DIdSet` of the `CodeGen` pass), names, in GHC's deterministic order:
  the closure's captured environment, what hsi copies into the heap object. A top-level closure has none.
- `[BINDER...]`: the arguments; none for a thunk. `(closure :u [] [] e)` is a top-level CAF.
- `rcon` allocates a constructor (saturated, void arguments dropped: GHC's Note [Constructor applications in STG]);
  `{:number N}` is the `ConstructorNumber` when `-fdistinct-constructor-tables` numbers it; the ticks are profiling's.
  The cost-centre stack (`CostCentreStack`) and the result `Type` are left out (§11).

## 7. Expressions

```
(app FUN [ATOM...])                                      ; StgApp: FUN a name; no arguments: a variable's value
LITERAL                                                  ; StgLit
(con DATACON [ATOM...] [REPS...]? {:number N}?)          ; StgConApp; [REPS...] the [[PrimRep]] of an unboxed sum's
                                                         ;   or tuple's fields when GHC records them
(primop "OP" [ATOM...] {:tycon TYCON}?)                  ; StgOpApp (StgPrimOp op): OP the primop's occurrence name
                                                         ;   ("+#", "catch#"); {:tycon} for tagToEnum#'s result type
(primcall "LABEL" "UNIT" [ATOM...] REPS)                 ; StgOpApp (StgPrimCallOp (PrimCall lbl unit)): foreign import
                                                         ;   prim, a Cmm function; REPS the result's
(ffi TARGET CONV SAFETY [ATOM...] REPS)                  ; StgOpApp (StgFCallOp (CCall (CCallSpec t c s)) ty)
(case EXPR BINDER ALTTYPE [ALT...])                      ; StgCase
(let BIND EXPR)                                          ; StgLet
(letne BIND EXPR)                                        ; StgLetNoEscape
(tick TICK EXPR)                                         ; StgTick
```

- **Atoms** (`StgArg`): a name (`StgVarArg`) or a literal (`StgLitArg`). After unarisation `StgApp` and `StgOpApp`
  keep their void arguments (`:v` binders; GHC needs them to compare a call's arguments with an arity), `StgConApp`
  and `StgRhsCon` drop them.
- **Alternatives' type** (`AltType`): `(poly)` `PolyAlt`, `(multi N)` `MultiValAlt N` (an unboxed tuple's or sum's N
  values), `(alg TYCON)` `AlgAlt`, `(prim REP)` `PrimAlt`.
- **An alternative** (`GenStgAlt`): `(alt CON [BINDER...] EXPR)`, CON `:default` (`DEFAULT`), a data constructor's
  name (`DataAlt`) or a literal (`LitAlt`); the binders are the constructor's non-void fields (`MultiValAlt`: the
  unboxed tuple's values).
- **Foreign calls**: TARGET `(static "SYMBOL" {:unit "UNIT" :fun false}?)` (`StaticTarget`; `:unit` when GHC names the
  package, `:fun false` for a call through a data label) or `(dynamic)` (`DynamicTarget`: the first atom is the
  function's address); CONV `:ccall` `:capi` `:stdcall` `:prim` `:javascript` (`CCallConv`); SAFETY `:safe`
  (`PlaySafe`), `:interruptible` (`PlayInterruptible`), `:unsafe` (`PlayRisky`); REPS the result's representations
  (the call's `Type`), `:v` for a call returning nothing (`IO ()`'s `State#` only).
- **Ticks** (`StgTickish`): `(scc "CC" COUNT SCOPE)` (`ProfNote`, the cost centre's user name and the two flags),
  `(hpc "MODULE" N)` (`HpcTick`), `(break "ID" [NAME...])` (`Breakpoint`, GHCi's), `(src "FILE" L1 C1 L2 C2 "NAME")`
  (`SourceNote`, at `-g`). None occur at `-O0` and `-O2` without `-g`, `-prof`, `-fhpc` (§11).

## 8. Data constructors

```
(stg/data TYCON [(dc DATACON TAG [FIELD-REPS...] [MARK...] WORKER?) ...])
```

One form per algebraic type constructor the code generator receives for the module (the module's own data types and
classes' dictionary types; newtypes and type synonyms have no runtime constructors and are absent). Each `dc`:
`dataConTag` (1-based, the order of the declaration), one REPS per field of the worker (`dataConRepArgTys`: an
unpacked strict field is its components, a field of an unboxed tuple type several representations, a void field
`:v`), one MARK per field, `:strict` or `:lazy` (`dataConRepStrictness`: the worker evaluates a `:strict` field's
argument), and the worker's name when it differs from the constructor's (never in GHC 9.14.1's dumps of the boot
libraries). A type of another module is in that module's dump: hsi builds a constructor from its `dc` without GHC.

## 9. Core forms

`tools/hsdump stg --core` (or `core`) also writes `MODULE.core.ae`: the module's Core after the simplifier's last
pass (a static plugin's pass appended to the pipeline), for the REPL's types (HE7) and for debugging, not for eval:

```
(core/module {...})                       ; as stg/module
(bind [NAME "TYPE"] EXPR)  (rec (bind ...) ...)
NAME  LITERAL  (app EXPR [EXPR...])  (lam [[NAME "TYPE"]...] EXPR)  (let BIND EXPR)
(case EXPR [NAME "TYPE"] "TYPE" [(alt CON [[NAME "TYPE"]...] EXPR)...])
(cast EXPR "TYPE")  (tick "TICK" EXPR)  (type "TYPE")  (coercion "TYPE")
```

Types, a cast's coercion (its right-hand kind) and ticks are strings as GHC prints them on one line; names as §3
(numbered separately from the STG's). Core forms are not checked by `bin/hsi dumps` beyond being read.

## 10. The checklist

Every constructor of the data types the STG of GHC 9.14.1 is made of, and its form. `bin/hsi spec-check` extracts the
constructors of these declarations from the pinned source tarball and requires each to be named here.

| GHC 9.14.1 declaration | constructor | form |
|---|---|---|
| `GenStgTopBinding` (`GHC.Stg.Syntax`) | `StgTopLifted` | `(bind ...)`, `(rec ...)` |
| | `StgTopStringLit` | `(string BINDER "BYTES")` |
| `GenStgBinding` | `StgNonRec` | `(bind BINDER RHS)` |
| | `StgRec` | `(rec (bind ...) ...)` |
| `StgArg` | `StgVarArg` | a name |
| | `StgLitArg` | a literal |
| `GenStgExpr` | `StgApp` | `(app FUN [ATOM...])` |
| | `StgLit` | a literal |
| | `StgConApp` | `(con DATACON [ATOM...] ...)` |
| | `StgOpApp` | `(primop ...)`, `(primcall ...)`, `(ffi ...)` |
| | `StgCase` | `(case EXPR BINDER ALTTYPE [ALT...])` |
| | `StgLet` | `(let BIND EXPR)` |
| | `StgLetNoEscape` | `(letne BIND EXPR)` |
| | `StgTick` | `(tick TICK EXPR)` |
| `GenStgRhs` | `StgRhsClosure` | `(closure UPD [FREE...] [BINDER...] EXPR)` |
| | `StgRhsCon` | `(rcon DATACON [ATOM...] ...)` |
| `GenStgAlt` | `GenStgAlt` | `(alt CON [BINDER...] EXPR)` |
| `AltType` | `PolyAlt` | `(poly)` |
| | `MultiValAlt` | `(multi N)` |
| | `AlgAlt` | `(alg TYCON)` |
| | `PrimAlt` | `(prim REP)` |
| `UpdateFlag` | `ReEntrant` | `:r` |
| | `Updatable` | `:u` |
| | `SingleEntry` | `:s` |
| | `JumpedTo` | `:j` |
| `StgOp` | `StgPrimOp` | `(primop "OP" ...)` |
| | `StgPrimCallOp` | `(primcall "LABEL" "UNIT" ...)` |
| | `StgFCallOp` | `(ffi ...)` |
| `ConstructorNumber` | `NoNumber` | absent |
| | `Numbered` | `{:number N}` |
| `AltCon` (`GHC.Core`) | `DataAlt` | a constructor's name |
| | `LitAlt` | a literal |
| | `DEFAULT` | `:default` |
| `Literal` (`GHC.Types.Literal`) | `LitChar` | `(char N)` |
| | `LitNumber` | `(int N)` ... (`LitNumType`) |
| | `LitString` | `(str "BYTES")` |
| | `LitNullAddr` | `(nulladdr)` |
| | `LitRubbish` | `(rubbish :type REPS)` |
| | `LitFloat` | `(float 0xBITS)` |
| | `LitDouble` | `(double 0xBITS)` |
| | `LitLabel` | `(label "SYMBOL" :fun)` |
| `LitNumType` | `LitNumBigNat` | `(bignat N)` |
| | `LitNumInt` | `(int N)` |
| | `LitNumInt8` | `(int8 N)` |
| | `LitNumInt16` | `(int16 N)` |
| | `LitNumInt32` | `(int32 N)` |
| | `LitNumInt64` | `(int64 N)` |
| | `LitNumWord` | `(word N)` |
| | `LitNumWord8` | `(word8 N)` |
| | `LitNumWord16` | `(word16 N)` |
| | `LitNumWord32` | `(word32 N)` |
| | `LitNumWord64` | `(word64 N)` |
| `FunctionOrData` (`GHC.Types.Basic`) | `IsFunction` | `:fun` |
| | `IsData` | `:data` |
| `TypeOrConstraint` | `TypeLike` | `:type` |
| | `ConstraintLike` | `:constraint` |
| `PrimRep` (`GHC.Core.TyCon`) | `BoxedRep` | `:L` `:U` `:B` (`Levity`: `Lifted`, `Unlifted`, none) |
| | `Int8Rep` | `:int8` |
| | `Int16Rep` | `:int16` |
| | `Int32Rep` | `:int32` |
| | `Int64Rep` | `:int64` |
| | `IntRep` | `:int` |
| | `Word8Rep` | `:word8` |
| | `Word16Rep` | `:word16` |
| | `Word32Rep` | `:word32` |
| | `Word64Rep` | `:word64` |
| | `WordRep` | `:word` |
| | `AddrRep` | `:addr` |
| | `FloatRep` | `:float` |
| | `DoubleRep` | `:double` |
| | `VecRep` | `[:vec N ELEM]` |
| `PrimElemRep` | `Int8ElemRep` | `:int8` |
| | `Int16ElemRep` | `:int16` |
| | `Int32ElemRep` | `:int32` |
| | `Int64ElemRep` | `:int64` |
| | `Word8ElemRep` | `:word8` |
| | `Word16ElemRep` | `:word16` |
| | `Word32ElemRep` | `:word32` |
| | `Word64ElemRep` | `:word64` |
| | `FloatElemRep` | `:float` |
| | `DoubleElemRep` | `:double` |
| `Levity` (`GHC.Types.Basic`) | `Lifted` | `:L` |
| | `Unlifted` | `:U` |
| `CCallTarget` (`GHC.Types.ForeignCall`) | `StaticTarget` | `(static "SYMBOL" {...}?)` |
| | `DynamicTarget` | `(dynamic)` |
| `CCallConv` | `CCallConv` | `:ccall` |
| | `CApiConv` | `:capi` |
| | `StdCallConv` | `:stdcall` |
| | `PrimCallConv` | `:prim` |
| | `JavaScriptCallConv` | `:javascript` |
| `Safety` | `PlaySafe` | `:safe` |
| | `PlayInterruptible` | `:interruptible` |
| | `PlayRisky` | `:unsafe` |
| `GenTickish` (`GHC.Types.Tickish`) | `ProfNote` | `(scc "CC" COUNT SCOPE)` |
| | `HpcTick` | `(hpc "MODULE" N)` |
| | `Breakpoint` | `(break "ID" [NAME...])` |
| | `SourceNote` | `(src "FILE" L1 C1 L2 C2 "NAME")` |
| `StrictnessMark` (`GHC.Core.DataCon`) | `MarkedStrict` | `:strict` |
| | `NotMarkedStrict` | `:lazy` |

The heads, by place: top level `stg/module` `stg/data` `string` `bind` `rec`; in `stg/data` `dc`; right-hand sides
`closure` `rcon`; expressions `app` `con` `primop` `primcall` `ffi` `case` `let` `letne` `tick` and the literals' `char`
`int` `int8` `int16` `int32` `int64` `word` `word8` `word16` `word32` `word64` `bignat` `str` `nulladdr` `rubbish`
`float` `double` `label`; alternatives `alt`; alternatives' types `poly` `multi` `alg` `prim`; foreign targets `static`
`dynamic`; ticks `scc` `hpc` `break` `src`.

## 11. What the forms leave out

- **Types.** STG is untyped but for representations; a binder's type, a closure's result type, `StgOpApp`'s result
  type (but its REPS for `primcall` and `ffi`, and `tagToEnum#`'s type constructor) are not written. The REPL's
  types come from Core forms (§9) or GHC (HES18).
- **Cost-centre stacks** (`StgRhsClosure`'s and `StgRhsCon`'s `CostCentreStack`, `CollectedCCs`): profiling is a class
  in v1 (survey §6.1).
- **The info table provenance map** (`InfoTableProvMap`, `-finfo-table-map`), **`ModuleLFInfos`** and the code
  generator's configuration: the native back end's business.
- **Foreign stubs**: a module's `foreign export` and `foreign import ccall "wrapper"` C stubs and a `capi` call's C
  wrapper (`CgGuts`' `ForeignStubs`) do not reach the hook. `capi` calls are `ffi` forms with CONV `:capi` and the C
  function's name; `foreign export` and wrappers are classes in v1 (survey §6.6).
- **The objects**: the hook passes the bindings on to GHC's own code generator unchanged, so the compilation writes
  the module's `.o` and `.hi` as without the helper (the `.hi` is what `bin/hsi dumps` compares with the installed
  libraries', HES6); the hook's result has no deterministic-unique renaming map (GHC passes `emptyDetUFM` with a
  hook), which can change the object's local labels, not the interface.

## 12. Choices

- **STG-S1** Names as strings (§3), not symbols with an escaping: the reader takes any string, and an atom's kind is
  clear from its type (string: a name; list: a literal).
- **STG-S2** External names `"UNIT:MODULE/OCC"`: one string per name, split without ambiguity (§3); the unit id is the
  real one (`containers-0.8-b42e`), as the libraries' symbols and `HasCallStack` text have it.
- **STG-S3** Internal names numbered per module in the dump's order (§3), top-level binders first: deterministic, and a
  change in one binding renumbers the bindings after it only from its own names on.
- **STG-S4** Representations as keywords (`:L`, `:int`), `:v` for none: short (the dumps are large, §4.2 of the survey),
  and every binder has them.
- **STG-S5** Free variables written (`closure`'s second field): the code generator's own list, so hsi need not compute
  them, and the AOT gets GHC's order.
- **STG-S6** Literals as lists, floating-point ones by their bits (§5): exact, and the same value GHC's code generator
  emits.
- **STG-S7** `con` (an expression: return a constructor) and `rcon` (a right-hand side: allocate one) are different
  heads, so a head says its place.
- **STG-S8** Data constructors in `stg/data` forms per module (§8), from the type constructors the hook receives: hsi
  builds constructors and their workers without GHC.
- **STG-S9** Core forms (§9) with types as GHC's text: for people and the REPL; no checker beyond the reader.
- **STG-S10** One file per module named after the module (`Data.Map.Internal.stg.ae`), one directory per package
  (`stg/containers/`), the samples one directory each (`stg/samples/Hello/Main.stg.ae`).
- **STG-S11** The header's `:flags` are the flags that decide the STG (`-outputdir`, `-j` dropped): two runs into
  different directories write the same bytes.
- **STG-S13** A representation-polymorphic type's REPS are `:rp` (§4) rather than a failure: GHC's own code
  generator never asks for them, so they are binders and fields no code moves; hsi treats one as a run-time class.
- **STG-S12** The hook runs GHC's own code generator after writing the forms, rather than stopping (an empty Cmm
  stream): the compilation's interfaces are then the installed libraries' (HES6's check), and the cost is the code
  generation's seconds.

## Appendix A: a worked example

`test/haskell/eval/samples/Hello.hs` (`main = putStrLn "Hello, Arbace"`) at `-O0`, its `Main.stg.ae` (the header's
hash shortened):

```
(stg/module {:unit "main" :module "Main" :file "Hello.hs" :ghc "9.14.1" :helper 4 :helper-hash "sha256:..." :flags ["-O0"]})
(string ["$trModule3_0" :addr] "Main")
(string ["$trModule1_1" :addr] "main")
(bind ["main_sat_2" :L] (closure :u [] [] (case (str "Hello, Arbace") ["main_sat_5" :addr] (prim :addr) [(alt :default [] (app "ghc-internal:GHC.Internal.CString/unpackCString#" ["main_sat_5"]))])))
(bind ["main:Main/main" :L] (closure :u [] [] (app "ghc-internal:GHC.Internal.System.IO/putStrLn" ["main_sat_2"])))
(bind ["main::Main/main" :L] (closure :u [] [] (app "ghc-internal:GHC.Internal.TopHandler/runMainIO" ["main:Main/main"])))
(bind ["$trModule2_3" :L] (rcon "ghc-internal:GHC.Internal.Types/TrNameS" ["$trModule1_1"]))
(bind ["$trModule4_4" :L] (rcon "ghc-internal:GHC.Internal.Types/TrNameS" ["$trModule3_0"]))
(bind ["main:Main/$trModule" :L] (rcon "ghc-internal:GHC.Internal.Types/Module" ["$trModule2_3" "$trModule4_4"]))
```

Read: the string literal `"Hello, Arbace"#` is an `Addr#` (`:addr`); the thunk `main_sat_2` (`:u`, no free variables,
no arguments: a CAF) unpacks it to a `String` when forced; `main` is the thunk `putStrLn main_sat_2`; the entry
`:Main.main` runs it under the top handler, which flushes the handles and turns an uncaught exception into GHC's
message and exit status (survey §2.2, E6). The `$trModule` bindings are the module's `Typeable` representation.
