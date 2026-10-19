# GR9: interpreted types as the runtime sees them, and generics at the REPL — survey and spec draft

Status: survey and spec draft, 2026-10-19. It is step 3 of the plan to the milestone
`arbace-interprets-go-v1` (doc/AGENDA.md, "Next"). Nothing here is adopted. The proposals
GC65-GC74 (§7) belong to the user; the text follows their recommendations.

It has two parts:

- **Part 1, GS8: the runtime overlay for named types** (GOI-SPEC.md §5.2, §5.3, G8, GS8, GS9, S36,
  S37, S38, S39; GC20, GC39). Compiled code and `reflect` would see an interpreted named type as a
  runtime type of its own, made at run time, with its method set. Today they see a proxy, a
  wrapper or an RT.
- **Part 2, GC34 (b): REPL vars for generic functions.** Arbace would call `slices.Index` and the
  program's own generic functions directly, with the type arguments inferred from each call's
  arguments.

Everything was measured on main at `77370a70`, with TamaGo's go1.27.1 (`/root/tamago-go`),
`CGO_ENABLED=0`. Native runs are linux/amd64. linux/arm64 runs used user-mode qemu on the same
host, so its absolute times are an emulator's. The stage 0 was the main checkout's
(`target/seed/amd64/arbace`, 135,504,032 bytes). The experiments' scratch is in
`.tmp/goi-gr9-survey/` and is not tracked. Appendix A describes the experiments; Appendix B holds
the prototype's overlay file in full, since it is the spec's main evidence. Prior art was read from
the module proxy's zip of `github.com/goplus/reflectx@v1.8.3` (downloaded into the scratch
directory) and from the host's module cache (`github.com/traefik/yaegi@v0.16.1`). Nothing was
vendored or fetched into the repo.

## Executive summary

- **The known entries.** Eleven programs of `test/goi/check-known.edn` are known because of G8:
  - `fixedbugs/bug177.go`, `bug468.go`, `issue15528.go`, `issue30041.go` and `issue77779.go`;
  - `recover2.go`, `genmeth1.go`;
  - `typeparam/issue49547.go`, `issue54456.go`, `mdempsky/17.go` and `nested.go`.

  A twelfth, `fixedbugs/issue4585.go`, is known because a reflect-made struct's `Equal` compares
  its blank fields. GS8's own `Equal` would fix that too (§3.6). `reflectmethod2.go` and
  `reflectmethod3.go` belong to GR8 (S39) and are not counted here.
- **The runtime already supports types made at run time.** Every offset in a type descriptor (a
  name, a type, a method's code) is resolved through `runtime.reflectOffs` when the descriptor is
  not in a module. That covers `resolveNameOff`, `resolveTypeOff` and `(rtype).textOff` in
  `runtime/type.go`. `reflect.StructOf` already uses this to make uncommon types with methods.
  `itabInit` (`runtime/iface.go`) matches methods by resolved name and type pointer, and it calls
  the code that `textOff` gives.

  So a named type with a method set, made at run time, works with compiled code if three things
  hold:
  - its descriptor has the layout gc's descriptors have;
  - its methods' types are the canonical `FuncOf` types;
  - each method's `Ifn` is code with the right calling convention.
- **The prototype (X9, Appendix A)** puts one file into package `reflect` as an overlay, the way
  `overlay/go` already patches the runtime. The file defines `ArbaceNamed`, `ArbaceSetLayout`,
  `ArbaceSetUnderlying` and `ArbaceSetMethods` in 204 lines. Methods are typed Go stubs, one per
  signature and slot, each calling the target its slot holds.

  It passes 34 of 34 checks on linux/amd64 and linux/arm64, and `reflect` with the file compiles
  for tamago/amd64 and tamago/arm64. The checks cover each known entry's mechanism:
  - `%T`, `%v`, `%#v`, `Name`, `PkgPath`, `String`;
  - `io.ReadAll` and `sort.Sort` through interpreted methods, compiled code's assertions;
  - `AssignableTo` both ways, `StructField.Anonymous`, a promoted method through
    `Field(0).Interface()`;
  - `MakeFunc` to a named func type while the collector runs;
  - the runtime's messages naming `main.T`;
  - 200,000 values and a 200,000-node list of a recursive type after five collections;
  - recursion through a map;
  - two generations that print alike and stay distinct;
  - an instance's name `main.F[main.foo]`;
  - `json` with an interpreted `MarshalJSON`.
- **Costs.**
  - Making a type with its pointer type and one method takes 9.6 µs.
  - A call through an itab into a stub costs 3.2-3.8 ns, against 1.6-2.1 ns for a compiled
    method. That is one indirect call more; the interpreted body costs far more.
  - `sort.Sort` of 10^5 through a run-time type takes 15.0-16.1 ms. A wrapper of func fields, like
    today's wrappers, takes 16.0-17.3 ms, and compiled `sort.IntSlice` 12.9-13.3 ms.
  - A stub costs about 225 bytes of executable: 1,000 stubs add 225,280 bytes, 10,000 add
    2,265,088. The table's 63 wrappers have about 60 distinct method signatures. With 16 slots per
    signature and 256 for `func() string` and `func() error`, that is about 1,440 stubs, about
    325 KB (0.24% of the executable).
- **Options compared (§2).** Four were compared:
  - (A) the reflect overlay, recommended;
  - (B) the same code outside reflect, through `go:linkname` to `reflect.addReflectOff` and copied
    `internal/abi` layouts (reflectx's route);
  - (C) a type descriptor module goi writes (goloader's route);
  - (D) today's representations, kept.

  For methods, three were compared:
  - (M1) typed stubs per signature and slot, recommended;
  - (M2) assembly trampolines into `reflect.callReflect`, reflectx's `icall`, as a later general
    fallback;
  - (M3) a compiler patch so itabs can hold closures, reflectx's `ifacefuncval`, excluded.
- **What GS8 removes.** Every interpreted named type becomes faithful (§5.2). The proxies (S38),
  the wrappers (§5.3 rule 3, §6.4) and `Proxy.Format` go. S37's tags go, S36's plain embedded fields
  go, and so do GR7a's shaped Go-memory representation (GC38) and most of GC20's special cases. An
  interface value's content is always the real value. Interpreted code finds the descriptor from
  the content's runtime type, through the registry and inline caches.
- **Part 2 (§4).** A generic function's var holds a `GoGenericFn`. Its `invoke` types the
  arguments: a Go value by its Go type, an Arbace scalar as an **untyped constant** of its kind
  (`Long` untyped int, `Double` untyped float ...). It then runs the checker's `infer`, as for a
  Go call with those arguments.
  - So `long → int or int64?` is answered by Go's own rule: `int`, unless another argument fixes
    the type parameter.
  - Instances are cached per var, by the arguments' type keys, and shared with the builder's
    `instance-cell`.
  - Explicit type arguments are `(go/inst go.slices/Index (slice int64))`.
  - The program's own generic functions, the interpretable packages (`slices`, `maps`, `cmp`,
    `iter`) and the listed packages' generic declarations (§9.3) take one path.
- **Effort.** 9-13 days in four milestones (§6): GR9a, the overlay, named types and stubs; GR9b,
  the representation without proxies; GR9c, names, embedding, recursion and `Equal`; GR9d, the
  generic vars.

## 1. What fails today, and why

### 1.1 The known entries

| program | what it checks | today's representation (GOI-SPEC §5.2, §5.3) | what GS8 gives |
|---|---|---|---|
| `fixedbugs/bug177.go` | `reflect.TypeOf(S2{}).Field(0).Anonymous`, `FieldByName("S1")` | S36: the embedded field is a plain field (`StructOf` refuses embedded fields with methods) | the struct's field marked embedded in a descriptor goi makes; promoted methods are goi's thunks (GR4) |
| `fixedbugs/bug468.go` | `p1.S` and `struct{X, Y int}` `AssignableTo` each other | S37: `p1.S`'s RT is a struct with a tag, so the underlying types differ | `p1.S` named, its underlying `struct{X, Y int}` untagged: `directlyAssignable` holds |
| `fixedbugs/issue15528.go` | `%T` and `%#v` of 23 values: `main.Int`, `*main.RWS`, interpreted interfaces nesting compiled ones | proxies, wrappers, RTs (GS9 not built) | `%T` is the descriptor's `Str`: `main.Int`, `*main.RWS`; `%#v` `&main.RWS{}` |
| `fixedbugs/issue30041.go` | `reflect.MakeFunc(reflect.TypeOf(fn), rf).Interface().(ft)` while the collector runs | `ft`'s values are proxies, so a `MakeFunc` value of RT `func() *int` is not an `ft` | `MakeFunc` of `ft`'s own descriptor; interpreted code's assertion finds `ft` by the content's type |
| `fixedbugs/issue77779.go` | `reflect.ValueOf(c).Field(0).Interface().(Renderer).Render()` with an embedded empty struct | a named empty struct's RT is `struct {}`, no type of its own | `main.ZeroSize` has its own descriptor and method set |
| `recover2.go` | the panic message `hash of unhashable type main.T` | the message names the RT (`struct { a int "goi:..." ... }`) | the runtime prints the descriptor's `Str` |
| `genmeth1.go` | `%T` of local types' values (`T1`...`T4`) in generic methods | proxies of an int8...int64 named type | `main.T1`... |
| `typeparam/issue49547.go` | `%T` of `F[foo]{}` is `main.F[main.foo]` | a struct RT with a tag | `Str` is the instance's gc runtime name (GC31) |
| `typeparam/issue54456.go` | two local `T[int]` of two functions: unequal, both `main.T[int]` | the RT of `int` | two descriptors, one name |
| `typeparam/mdempsky/17.go` | `reflect.TypeOf(x).Name()` is `V` or `K` for values ranged over in generic functions | `V`'s RT is `int` | `V` |
| `typeparam/nested.go` | the identities and names of local generic types' instances inside generic functions (`main.T[int;main.U[int;int]·3]`) | RTs of the underlying types | descriptors per instance, named after gc's `mangle` (§3.5) |
| `fixedbugs/issue4585.go` (bonus) | a map keyed by structs with blank fields written through `unsafe` | `StructOf`'s `Equal` compares the blank fields | a named type's `Equal` is goi's, which skips blank fields as gc's does |

### 1.2 Why the representations are what they are

GR3-GR7a had `reflect`'s constructors and no others. reflect cannot make:
- a named type (golang/go#16522);
- a type with methods, except a struct embedding one type with methods as its only field;
- an interface type (golang/go#4146).

Embedded fields with methods are possible only in that one case (golang/go#15924, #38783). So goi
used descriptors of its own. Named structs got tagged RTs (S37). Other named types share their
underlying types' RTs, and their values with methods are proxies in interfaces (S38). Compiled
interfaces get the table's wrappers (§5.3 rule 3, GC22). Recursive types are shapes in Go memory
(GR7a, GC38). Each of these is visible to compiled code and reflect.

## 2. How a named type can get a runtime type of its own

### 2.1 What the runtime and the linker assume

Read in go1.27.1 (`/root/tamago-go/src`):

- **Descriptors** (`internal/abi/type.go`): `abi.Type` has the following fields:
  - `Size_`, `PtrBytes`, `Hash`, `TFlag`, `Align_`, `FieldAlign_`, `Kind_`;
  - `Equal`, `GCData`;
  - `Str` (a `NameOff`) and `PtrToThis` (a `TypeOff`).

  A kind's header (`StructType`, `FuncType`, `PtrType` ...) follows `abi.Type`, then the
  `UncommonType` when `TFlagUncommon` is set (`PkgPath`, `Mcount`, `Xcount`, `Moff`). The
  `[Mcount]abi.Method` array starts at `Moff` past the uncommon type. Each method has `Name`,
  `Mtyp`, `Ifn` and `Tfn`, and the exported methods come first. A function type's parameter types
  follow its uncommon type (`FuncType.InSlice`).

  A named type has `TFlagNamed`, and its `Str` is `"*pkg.T"` with `TFlagExtraStar`, so that `*T`
  can share the string. "Multiple types may have the same value of GCData, including when
  TFlagGCMaskOnDemand is set" (the comment on `GCData`).
- **Offsets made at run time** (`runtime/type.go`, the comment on `reflectOffs`). When the base
  pointer is in no module, `resolveNameOff`, `resolveTypeOff` and `(rtype).textOff` look the offset
  up in `reflectOffs.m`. Its entries come from `reflect.addReflectOff`, which `runtime1.go`
  implements: negative identifiers, and a pinned pointer each, which also keeps a heap descriptor
  alive.

  `reflect.addReflectOff` carries a bare `//go:linkname` ("Notable members of the hall of shame
  include: github.com/goplus/reflectx", `reflect/type.go`). So code outside `reflect` may pull it.
- **itabs** (`runtime/iface.go`, `itabInit`).
  - A type's methods and an interface's are walked in step, both sorted alike: exported first,
    then by name (cmd/compile's `types.Sym.Less`).
  - A method matches when `rtyp.typeOff(t.Mtyp) == itype` (pointer identity) and the names are
    equal; an unexported name also compares package paths, its own or else the uncommon type's
    `PkgPath`.
  - `itab.Fun[k]` is `textOff(t.Ifn)`. A compiled interface call is `CALL itab.Fun[k]` with the
    data word as the first argument, so **`Ifn` receives the receiver as one word**: the value
    itself for a pointer-shaped type (`TFlagDirectIface`), else a pointer to the value.
  - **itabs are cached for good**, including the failed ones (`getitab` adds an itab whose
    `Fun[0]` is 0). So a method set may not change once an itab of the type exists.
- **`Tfn`** is used by compiled calls of method expressions, which compiled code cannot write for
  an interpreted type, and by `reflect.Type.Method(i).Func`, which calls it with the receiver by
  value. `reflect.Value.Method` calls `Ifn` (`methodReceiver`, `reflect/value.go:905`).
- **`typelinks`** list a module's types for `typesByString`. reflect's constructors
  (`SliceOf`, `MapOf`, `FuncOf`, `StructOf`, `ptrTo`) use it to return a compiled type equal to
  the one asked for. A run-time type is in no `typelinks`, so `reflect` and the runtime cannot
  confuse it with a compiled type of the same name. Every such lookup also compares element type
  pointers (`p.Elem != &t.t`) or `haveIdenticalUnderlyingType`, whose components compare by
  pointer.
- **`typesEqual`** and the module `typemap` only concern several modules (plugins, shared
  libraries), which Arbace has none of.
- **The collector** scans a value by its type's `PtrBytes` and `GCData`, which a named type copies
  from its underlying type: one layout, one mask. A descriptor allocated in the heap must be
  scanned as what it is (its `Fields` slice, `GCData`, `Equal`). The prototype allocates each at
  a Go type made by `StructOf` of reflect's own header types (`structType`, `uncommonType`, the
  method array), as `StructOf` does for itself.
- **Stack scanning of reflect's stubs** (`runtime/stkframe.go`, `argMapInternal`). Only frames of
  `reflect.makeFuncStub` and `reflect.methodValueCall` get their argument maps and their `RegArgs`
  stack object from the `methodValue` in their frame. Any other assembly stub that spills register
  arguments (reflectx's `icall` copies) is scanned with `NO_LOCAL_POINTERS` and no argument map.
  That matters for M2 (§2.3).

### 2.2 The routes to a descriptor

- **(A) The reflect overlay (recommended).** One file added to package `reflect` by the build's
  overlay. `bin/jrt overlay` already writes one for the runtime's `proc.go`, `runtime2.go` and
  `arbace_local.go` (BUILD.md, amendment B5), and so does the box's (`bin/lib/box-overlay.bash`).
  The file uses reflect's own types and functions: `structType`, `uncommonType`, `newName`,
  `resolveReflectName`, `resolveReflectType`, `resolveReflectText`, `toType` and `fnv1`. A change of
  `internal/abi` at a toolchain bump is then a compile error, not silent corruption.

  goi calls it as `reflect.ArbaceNamed` and so on (Appendix B). The file is Go forms like
  `overlay/go/runtime`, printed at build time, so its source is Arbace's as the rest of the
  overlay is. In the prototype it is 204 lines of Go.
- **(B) Outside reflect, through linkname.** reflectx's route (`rtype.go`: `newType`,
  `setTypeName`, `SetUnderlying`; `name.go`: its own name encoder). It needs:
  - `//go:linkname` to `reflect.addReflectOff`, which is pullable;
  - copies of `internal/abi`'s layouts and of the name encoding;
  - unexported reflect helpers through linkname, which go1.23's linker checks refuse unless they
    are push-marked. reflectx's README offers `-ldflags=-checklinkname=0` for that.

  It needs no overlay, but the layouts are unchecked copies, and Arbace has the overlay anyway.
- **(C) A type descriptor module goi writes.** goloader's technique: a `moduledata` of goi's own
  holding `types`/`etypes`, `typelinks`, `itablinks` and a `pclntab`, linked into
  `firstmoduledata`'s list, so that offsets resolve within it. It is the most faithful (real
  `typelinks`), but it is tied to the runtime's module layout and its many invariants, which
  `typesEqual` and the module checks assume for code too. A module of types without code is not a
  shape the runtime expects. And it gives nothing that `reflectOffs` does not already give.
- **(D) Keep today's representations** (GS8 not built): the eleven entries stay known, and GS9's
  `%T` override would mend only `%T` of interpreted code's own `fmt` calls.

### 2.3 Methods: the code behind `Ifn`

A method's `Ifn` must be a code address. A call through it carries no closure context: on amd64
`DX` is not set by an interface call, and on arm64 `R26` is not. So each (type, method) needs code
of its own, which knows its target without being told. The options:

- **(M1) Typed Go stubs per signature and slot (recommended).** For each method signature S in a
  set the table fixes, the generator writes K Go functions `stub_S_k(p unsafe.Pointer, args...)
  results`, each calling `slot_S[k]`.
  - The function has the method's own calling convention: the receiver word first, then the
    arguments.
  - A slot holds goi's target (the type's descriptor and the method's cell, entered as GR5's
    `EnterMethod` does). Redefining a method's body changes what the slot reaches (its cell), not
    the code address, which a cached itab keeps (X9: replacing `Ifn` after the itab is made has no
    effect).
  - Sound for the collector: a stub is an ordinary Go function with its own stack maps, the
    receiver typed `unsafe.Pointer`, a pointer word in either case.
  - No assembly.
  - The set S: the method signatures of the interfaces the table lists (its wrappers' methods
    today, about 60 distinct signatures among about 180 methods), which are the interfaces compiled
    code holds interpreted values in. The fixed common ones are also included (`String() string`,
    `Error() string`, `Format(fmt.State, rune)`, `GoString() string`, `MarshalJSON`...).
  - A method whose signature has no family gets a panic stub: a no-argument Go function that
    panics with goi's message, safe whatever its caller passed. That is the run-time form of
    today's check-time error, "the interface has no wrapper in the table". Interpreted code calls
    such a method by its descriptor as before; only compiled code through an itab (or
    `Value.Method`) meets the panic.
- **(M2) Assembly trampolines, reflectx's `icall`** (`icall/icall4096/icall_regabi_go127_amd64.s`
  and `_arm64.s`: `MAKE_FUNC_FN(NAME, INDEX)` copies of `makeFuncStub`, each passing its index to
  `i_x`, which calls `reflect.callReflect` on the `makeFuncImpl` its slot holds).
  - Any signature, at `reflect.MakeFunc`'s cost: X2 measured 319 ns per call.
  - Assembly per architecture, about 110 KB of source per 4,096 slots and architecture.
  - The runtime's frame rule (§2.1, `argMapInternal`) must be extended to the trampolines' names
    by the runtime overlay, or a collection during a call misses the spilled pointer arguments.
    issue30041.go tests exactly that class of bug.

  It is the general fallback, for later, if the check finds compiled code calling interpreted
  methods of signatures beyond the table's.
- **(M3) A compiler patch, reflectx's `ifacefuncval`** (`cmd/iface_patch/README.md`). itabs would
  hold tagged `makeFuncImpl` pointers, and every interface call site would unwrap them
  (`cmd/compile/internal/amd64`, `arm64`, `runtime/asm_*.s`). It needs a patched gc and costs a
  test at every interface call of the whole program. Excluded: Arbace does not patch gc's code
  generation.

**Slots** (GC67). A slot is used for good: itabs are never freed, and the descriptor is pinned
in `reflectOffs`. A value method of a type T that is not pointer-shaped uses one slot for T's and
`*T`'s entries alike, since both receive a pointer. A pointer-shaped T's value methods need two.
Each REPL generation of a type with methods takes new slots. The proposal: 256 slots for
`func() string` and `func() error`, 16 for each other family, counts in `go/arbace/gotable.edn`.
That is about 1,440 stubs, about 325 KB (X9: 225 bytes a stub). When a family is exhausted, the
method gets the panic stub, and goi warns at the type's definition.

**`Tfn`.**
- For a pointer receiver and for a pointer-shaped T, `Tfn = Ifn` (the same word).
- For other value receivers, `Tfn` is `-1`, which the runtime resolves to `unreachableMethod`:
  `reflect.Type.Method(i).Func` of an interpreted type is GR8's targeted entry (doc/AGENDA.md, step
  2), which dispatches interpreted types by descriptor before reaching `reflect`.

### 2.4 Prior art

| | named types at run time | methods seen by compiled code | `%T`, reflect | source |
|---|---|---|---|---|
| **yaegi** v0.16.1 | no: `reflect.StructOf` and the other constructors (`interp/type.go`, `refType`) | wrappers generated per compiled interface (`_io_Reader{IValue; WRead}`) | different: README, "Limitations": "Representation of types by `reflect` and printing values using %T may give different results between compiled mode and interpreted mode"; "Interfaces to be used from the pre-compiled code can not be added dynamically" | the module cache's `github.com/traefik/yaegi@v0.16.1`: `README.md`, `interp/type.go` |
| **reflectx** v1.8.3 (ixgo's, formerly igop's) | yes: `NamedTypeOf`, `SetUnderlying`, `SetTypeName` copy a descriptor and set `TFlagNamed|TFlagExtraStar`, `Str`, `PkgPath` (`rtype.go`); `StructOf` marks embedded fields after `reflect.StructOf` (`setEmbedded`) | `SetMethodSet`; `Ifn` from `icall` slots ("Default gc path: each installed method needs a unique ifn stub", README; `internal/icall512` by default, up to `icall20480`), or a patched gc (`ifacefuncval`) | gc's | `proxy.golang.org/github.com/goplus/reflectx/@v/v1.8.3.zip` (origin `683669fe520773240b3a1dfa7ef1a9fe0a209af8`, tag `v1.8.3`): `README.md`, `rtype.go`, `reflectx.go`, `methodof_icall.go`, `icall/icall4096/icall_regabi.go`, `icall_regabi_go127_amd64.s`, `cmd/iface_patch/README.md` |
| **ixgo** v1.2.3 (2026-10-13) | through reflectx | through reflectx (`icall`) | gc's | `proxy.golang.org/github.com/goplus/ixgo/@latest` (origin `a761e605548e65516bf99e374690456090771c76`); the REPL survey's §3 |
| **gomacro** | emulated named types (`xreflect`) | wrappers | different | the REPL survey's §3, §7 |
| **golang.org/x/tools/go/ssa/interp** | its own values, no reflect types | none (only its externals) | its own | the REPL survey's §3 |
| **goloader** | real, from gc's object files and a module it registers | real | real | the REPL survey's §3 (option d3) |
| **Go's `plugin`** | real | real | real | needs cgo and dynamic linking: unusable here (the REPL survey's §3) |
| **reflect itself** | `StructOf` makes uncommon types with methods for a single embedded field, its `Ifn`s the embedded type's own (`reflect/type.go`, `StructOf`) | yes in that case | `struct { ... }` names | go1.27.1 `src/reflect/type.go` |

So reflectx shows the descriptor technique works against gc's runtime (go1.21-1.27), and yaegi
shows what is lost without it. Arbace's difference from reflectx is threefold:
- the code lives inside `reflect`, by the overlay, not copied beside it;
- the methods are typed Go stubs from the table, not assembly;
- the descriptors follow goi's checker, generation by generation.

## 3. The design (spec draft, Part 1)

### 3.1 The overlay

`overlay/go/reflect.ae` declares the package file list of a new overlay file,
`overlay/go/reflect/arbace_named.ae`, Go forms of Appendix B's code, with these changes:
- a mutex around its maps;
- the name encoder of unexported methods with their package paths (`abi.Name`'s bit 2), which
  promoted unexported methods of another package need;
- `Equal` for named structs (§3.6).

`bin/jrt overlay` and the box's overlay add it beside the runtime's three files, for every
configuration (linux and tamago, amd64 and arm64). The API is four functions:

| function | does |
|---|---|
| `ArbaceNamed(pkgPath, str string, k Kind, nparams, nm, npm int) (T, PT Type)` | allocates T (kind k, room for nm methods; a function type's nparams parameters) and `*T` (room for npm), named `str` (gc's runtime name, §3.5) of package `pkgPath`, with a hash of its own, `T.PtrToThis` set to `*T` |
| `ArbaceSetLayout(T, shape Type)` | gives T, before its underlying type, its shape's header: layout, alignment, pointer mask, kind data (§3.4) |
| `ArbaceSetUnderlying(T, under Type, embedded []int)` | copies under's header (layout, mask, `Equal`, kind data; a function type's parameters), keeps T's name, hash, pointer type and uncommon type, marks the embedded fields |
| `ArbaceSetMethods(T Type, ms []ArbaceMethod)` | fills T's method table, exported first, by name: name, `Mtyp` (the `FuncOf` type), `Ifn`, `Tfn` |

### 3.2 Descriptors and RT (amending §5.2)

- **Every interpreted named type's RT is its own descriptor**, made by `ArbaceNamed` when its
  generation is made, completed by `ArbaceSetUnderlying` when its underlying type is known (goi's
  `TSetUnder`, `go/arbace/goi/types.ae`), its methods set when the method set is known (GR4's
  amendment 2). Struct, basic, pointer, slice, array, map, channel and function kinds alike.
  Composites of interpreted types are `reflect`'s constructors over those RTs (`[]Celsius` is
  `SliceOf(Celsius)`: `[]main.Celsius`).
- **S37's tags end.** A named struct's RT is its untagged underlying struct, renamed. Conversion,
  assignability and `fmt` are gc's (X9: `AssignableTo` both ways, `%+v`, `json`).
- **S36 ends.** Embedded fields are embedded (X9: `Anonymous`, `FieldByName`). The promoted methods
  are entries of the type's method set whose code is GR4's thunks, reached by stubs like any
  method.
- **Faithful types are all types.** Every interpreted named type and every composite of them has an
  RT of its own. The registry from RT to descriptor (`interp`, `interpRT`) covers every one. Only
  interface types keep `any` as RT (GC71).
- **Equal, hash.** A named type's `Hash` is fnv-1 of its name and a counter, distinct per
  generation. Its `Equal` is its underlying type's, except for structs with blank fields (§3.6).

### 3.3 Interface values and method dispatch (amending §5.3)

- **An interface value's content is the real value of RT(D) for every D.** Rule 2's proxies and
  rule 3's wrappers end. A value stored in Go memory of a compiled interface type I is converted by
  `reflect.Value.Convert` (the runtime's `getitab`) to I's word pair, through D's own method set.
  The check-time error "the interface has no wrapper in the table" ends. Its place is the panic
  stub of a signature without a family (§2.3): a check-time warning when the checker sees a value
  of such a type converted to a compiled interface that has the method, else a run-time panic
  naming the method.
- **Inside interpreted code** the dynamic type of a content is the registry's descriptor of its
  `reflect.TypeOf`, else the compiled type's. Method calls, assertions and type switches key their
  inline caches by the RT pointer (one comparison). GR4's amendments 4 and 5 (`contentOf`,
  `proxyOf`, the raw values of GC20) are no longer needed: a value is never anything but itself.
- **fmt** finds `String`, `Error`, `Format` and `GoString` through compiled assertions, so
  `Proxy.Format`'s re-implementation of fmt's order ends (X9: `Println` of a `Celsius`, `%v` of
  `*RWS`). GR5's `goString` (§5.7) still names an old generation `#n` at the REPL. That is goi's
  printing, not fmt's.
- **`%T`** of any interpreted value is gc's (GS9 becomes moot).

### 3.4 Recursive types (amending GR7a)

A named type's RT exists before its underlying type is built, so a type reaching itself through a
pointer, slice, function or channel needs nothing more. X9 builds `type Node struct{Next *Node; V
int}` from `*Node` before `Node` is complete, and `type M map[string]M`.

reflect's `MapOf` reads its key and element types' layouts (its group type), and `StructOf` and
`ArrayOf` read their fields' and elements' layouts. So a type whose layout is asked for before it
is complete gets its **shape** first (`ArbaceSetLayout`; GR7a's `shape-type`, which is already
the layout and pointer words). That is the case of a type reached through a map's key or element,
or by value from such a type. X9 checks `T struct{M map[string]U; N int}` with `U struct{T T; P
*int}`: sizes 16 and 24, 50,000 entries intact after two collections.

GR7a's shaped Go-memory representation, `reshape` and `TShape`'s second RT then go: one RT per
type, in frames and in memory (GC70).

### 3.5 Names and identities

- `Str` is gc's runtime name. A package-level type is `pkgname.T`. A local type in a non-generic
  function is `pkgname.T`, gc trimming the `·N` suffix (cmd/compile `types/fmt.go`, `tconv`: "In
  unified IR, function-scope defined types will have a ·N suffix ... Trim this off for
  non-fmtTypeID modes").
- A generic type's instance is named as GC31 has it (`main.F[main.foo]`). A type local to a generic
  function is named by noder's `mangle` (`cmd/compile/internal/noder/reader.go`): the enclosing
  function's type arguments, `;`, the type's own, with the `·N` suffix kept inside
  (`main.T[int;main.U[int;int]·3]`). That is `nested.go`'s expected output; goi computes `N` as
  the noder numbers a function's local types.
- `PkgPath` is the package's path. A directory test's local packages have their import paths
  (`bug468.go`'s `p1`).
- Identity is the descriptor's. Two generations of `main.P` are two types that print alike (X9:
  `P#1 == P#2` false, their values unequal, two map keys, both `%T` `main.P`). Only goi's
  `goString` adds `#n`. That is the one place a user sees generations; a gc program cannot have two
  either way.

### 3.6 Equality of structs with blank fields

reflect's `StructOf` gives a struct type an `Equal` that compares every field, blank fields
included (`reflect/type.go`, `StructOf`, `typ.Equal`), while gc's skips them. The runtime's
`typehash` already skips them (`isRegularMemory` refuses `_`). `ArbaceSetUnderlying` gives a named
struct with blank fields goi's own `Equal`: the non-blank fields' `Equal`s at their offsets. That
fixes `issue4585.go` for named types. Unnamed struct types made by `StructOf` keep the difference
unless the overlay also corrects `StructOf`'s `Equal`, a one-line change of reflect. That is left
for GR9c to decide by the check.

### 3.7 Generations at the REPL (amending §2.5, S8)

A type's generation changes when its underlying type changes (today), **or when its method set's
names or signatures change** (new). The method array is allocated with the type, and cached itabs,
including failed ones, fix what an interface sees (§2.1). A method's body redefined with the same
signature keeps the generation: its slot reaches the method's cell, which late-binds (G9). S8 is
amended accordingly (GC68).

### 3.8 What stays

- Interface types of interpreted packages keep RT `any` in Go memory (GC71).
- `unsafe` layout that gc decides stays out of scope.
- The table keeps its invokers and adapters (§6.4). Its wrappers go, and its stub families come.
- GS10's interop and GS7's protocols are unchanged.
- S39 is unchanged: goi never looks a method up by a name that is not constant.

## 4. Part 2: REPL vars for generic functions (GC34 (b))

### 4.1 Today

A generic function or method has no cell and no var (`arbace/go/repl.ae`, `define!`: "a generic
function or method has no cell: its instances have"). Arbace reaches generic code only through a
Go function written for it (`IndexInts` in X10) or through `go/eval`, which checks the REPL's
package anew each time: 139-144 ms per call in X10, against 1.5 µs for a var's call.

### 4.2 The var

`define!` gives a generic function `F` (and a generic type's method `T.M`, §4.6) a var `F` whose
root is a `GoGenericFn`, an `IFn` holding:
- the generic object;
- its package's build context (`:ctx-of`);
- an instance cache (§4.4).

On `invoke(args)`:

1. **Type the arguments** (§4.3) into checker operands `{:type :mode :val}`.
2. **Look up the cache** by the operands' type keys. On a hit, call the cached instance's `GoFn`.
3. **On a miss**, run the checker's `infer` (`arbace/go/check/infer.ae`; go1.27.1's `infer.go`,
   `unify.go`) with the function's type parameters, any explicit type arguments (§4.5), the
   signature's parameters (variadic ones expanded as `call-args` does) and the operands.
   - Untyped operands get their default types, and the constraints infer the rest.
   - A failure is the checker's error, gc's words (`in call to slices.Index, cannot infer E`),
     thrown as the REPL's check errors are (§3.8: a `CompilerException` with `:go/errors`).
   - Then the instance's type arguments are verified against their constraints (the checker's
     `verify-later!`).
   - Then `instance-cell` (`arbace/go/build.ae`) gives the instance's cell, shared with the
     instances interpreted code makes. A `GoFn` is made over it, its parameters' conversions
     those of §5.6 for the instance's signature. It is cached and called.

The var is a var like a function's: redefining `F` at the REPL replaces its root (a new
`GoGenericFn`, an empty cache), and callers holding the var see the new one (G9).

### 4.3 Arbace values as operands

The rule: **an Arbace scalar is an untyped constant of its kind; a Go value has its Go type.**

| Arbace argument | operand for inference | then converted (§5.6) as |
|---|---|---|
| `Long`, `Integer`, `Short`, `Byte`, `BigInt` | untyped int constant, its value | the inferred type: out of range an error (§5.6) |
| `Double`, `Float`, `Ratio` | untyped float constant | the inferred type |
| `Character` | untyped rune constant | the inferred type |
| `Boolean` | untyped bool | the inferred type |
| `String` | untyped string | the inferred type |
| `nil` | untyped nil | the inferred type's zero (nil) |
| a Go value (`GoValue`) | typed, its descriptor's type (the registry's for an interpreted type, after GS8 any type) | itself |
| a Java object for an `any` parameter | typed `any` | itself |
| an Arbace fn | none: skipped like an untyped nil (it cannot be unified) | adapted to the inferred func type (§7.5) |
| an Arbace collection | none: skipped | §5.6's conversions (GS6: `go/slice`, `go/map` explicit) |

So **`long → int or int64?`** has Go's answer:
- `(go.slices/Index xs 7)` with `xs` a Go `[]int64` infers `S = []int64`, `E = int64`, and 7
  converts to `int64`;
- `(go.cmp/Compare 1 2)` infers `int`, the untyped int's default;
- `(go.cmp/Compare 1 2.5)` infers `float64` (go/types' `maxType`, the checker's `max-type`);
- `(go.slices/Max ...)` of Arbace numbers alone takes nothing, since a vector is not a slice
  (GS6).

Since `Long` is untyped, an Arbace caller cannot ask for `int64` by the value alone. That is what
explicit type arguments are for (§4.5). This mirrors §5.6, where a Go scalar reaching Arbace is
always a `Long`: a typed scalar does not exist on Arbace's side.

Values matter only through representability: a `Long` 300 for an `E` that another argument fixes
to `int8` is §5.6's out-of-range error, after inference.

### 4.4 Caching instances

- **The key.** One element per argument: the descriptor of a typed operand (interned, so `=` is
  identity), else the untyped kind's keyword. Values are not part of the key, since inference
  depends only on kinds and types. The key is computed by the `GoGenericFn` from the arguments'
  classes and `GoValue`s' types, without the checker.
- **The cache.** A monomorphic inline entry (the last key and instance) and a `ConcurrentHashMap`
  of keys to instances' `GoFn`s.
- **Instances are shared.** `instance-cell` interns by `[object (mapv desc targs)]`, so an
  instance first made by Arbace's call is the one interpreted code's call reaches, and the
  reverse. A redefinition of the generic function makes a new object, hence new cells.
- **The cost of a hit** is the key, one entry comparison and the instance's `GoFn` call, against
  that call's 1.5 µs (X10). **A miss** runs `infer` and the instance's build once per key.

### 4.5 Explicit type arguments

`(go/inst f T...)` is a macro. `f` names a generic function's or type's var, and each `T` is a Go
type form (doc/go/SPEC.md §5), resolved as `go/eval` resolves names. Outside a `go.*` namespace
that is `go.user`'s file: its imports, and qualified names like `time/Duration`.
- With every type argument given, it is the instance's `GoFn` (for a function) or `GoType` (for a
  generic type).
- With fewer, it is a `GoGenericFn` that infers the rest, its explicit arguments fixed (Go's
  partial instantiation `slices.Index[[]int64]`).

Examples:
- `((go/inst go.slices/Index (slice int64)) xs 7)`;
- `((go/inst go.cmp/Compare int64) 1 2)`;
- `(def push (go/inst go.main/List.Push int))`;
- `((go/inst go.main/List int) {})`, the zero `List[int]` through the `GoType`'s `IFn` (S46).

It mirrors Go forms' own `(inst F T...)`. A `GoType` is already an `IFn` for conversions (§7.1),
so `(go.main/List int)` cannot mean instantiation; `go/inst` keeps the two apart.

### 4.6 Methods of generic types, generic methods, compiled generics

- **Methods of a generic type** (`List[T].Push`). The var `List.Push` takes the receiver first, as
  GR5's method vars do, and infers the type's arguments from the receiver's instance descriptor
  (`List[int]`: `T = int`), then any of the method's own (go1.27's generic methods) from the rest.
- **The interpretable packages** (`cmp`, `iter`, `maps`, `slices`, §9.3) are interpreted packages,
  so their generic functions get vars by the same `define!`. `(require '[go.slices :as slices])`
  then `(slices/Index xs 7)`.
- **The listed packages' generic declarations** (`sync.OnceValue`, `atomic.Pointer[T]`'s methods;
  §9.3, the digest's bodies) get vars in their compiled namespaces, made on demand (S9) as
  `GoGenericFn`s over the digest's objects. An instance the list names compiled (`:instances`) is
  used for its key instead of an interpreted one. GC32's generic functions over internal packages
  (`reflect.TypeFor`) stay unsupported when called.

### 4.7 Tests and exit

`test/goi/repl.ae` gains checks for each of these:
- `slices.Index` over a Go `[]int` and a `[]int64`;
- `cmp.Compare` of `Long`s, of a `Long` and a `Double`, and explicit `int64`;
- `slices.SortFunc` with an Arbace fn;
- the program's own `Map[T, U any]`;
- a generic type's method;
- a redefinition seen by the var;
- the errors: cannot infer, constraint not satisfied, out of range.

The oracle's `forms/go.ae` gains cases.

## 5. Measurements

| | value | where |
|---|---|---|
| checks of the prototype | 34 of 34 on linux/amd64 and linux/arm64 (qemu); `reflect` with the overlay compiles for tamago/amd64 and tamago/arm64 | X9 |
| making a named type, its pointer type and one method | 9.6 µs (amd64), 76 µs (arm64 under qemu) | X9 |
| `String()` through `fmt.Stringer`, 10^7 calls | run-time type 3.15-3.77 ns, compiled 1.58-2.08 ns (amd64); 77-81 ns and 22-23 ns (qemu) | X9 |
| `sort.Sort` of 10^5 ints, best of 5 | run-time type's methods 15.0-16.1 ms, a wrapper of func fields 16.0-17.3 ms, compiled `sort.IntSlice` 12.9-13.3 ms (amd64) | X9 |
| a stub's size | 225 bytes (1,000 stubs +225,280 bytes; 10,000 +2,265,088; `-trimpath -ldflags=-s`) | X9 |
| the table's wrappers | about 180 methods, about 60 distinct signatures (linux/amd64's `target/arbace-go/gotable`) | X9 |
| the proposed families | about 1,440 stubs, about 325 KB, 0.24% of the executable's 135.5 MB | §2.3 |
| a var's call of a non-generic function calling `slices.Index[[]int,int]` | 1.49 µs; the same loop written for `[]int` 1.51 µs | X10 |
| `go/eval` of `slices.Index(...)` / of a non-generic call | 144 ms / 139 ms per call (the REPL package checked each time) | X10 |

The interpreted method's own cost (GR5's typed entry into a frame) dominates a stub's two
nanoseconds. So the speed of compiled code calling interpreted methods is today's wrappers'.
goi's own dispatch gains a little: an inline cache keyed by a type pointer, where a proxy needed
an unwrap first.

## 6. Milestones, exit criteria, effort

| milestone | builds | exit | days |
|---|---|---|---|
| **GR9a** the overlay and named types | `overlay/go/reflect/arbace_named.ae` (§3.1) in every configuration's overlay; `TNamed`/`TSetUnder` over it (§3.2); the table's stub families and slots (`gotable.edn`'s `:stubs`, the generator), `ArbaceSetMethods` from GR4's method sets; the registry by RT | X9's checks as `bin/goi test` programs; `bin/goi check` without regression | 3 |
| **GR9b** values are themselves | proxies, wrappers and `Proxy.Format` removed (§3.3); interface conversion to compiled interfaces by `getitab`; inline caches by RT; generations by method set (§3.7); the check-time wrapper error replaced | `issue15528.go`, `issue30041.go`, `issue77779.go`, `recover2.go`, `genmeth1.go`, `issue49547.go`, `mdempsky/17.go` pass; the oracle's `forms/go.ae` unchanged | 2-3 |
| **GR9c** names, embedding, recursion, equality | embedded fields (§3.2), gc's runtime names with noder's mangling (§3.5), shapes as layouts only (§3.4), `Equal` of blank fields (§3.6) | `bug177.go`, `bug468.go`, `issue54456.go`, `nested.go`, `issue4585.go` pass; GR7a's recursive programs still pass; the box's check | 2-3 |
| **GR9d** generic vars | `GoGenericFn`, the operands (§4.3), the cache (§4.4), `go/inst` (§4.5), methods of generic types and the listed generics (§4.6) | §4.7's REPL checks and oracle cases; `slices.Index` called directly | 2-3 |
| the record | GOI-SPEC's "As built: GR9"; `check-known.edn` without the entries | | 0.5 |

**Exit criteria (GR9).**
- The eleven entries of §1.1 leave `test/goi/check-known.edn` and pass, and `issue4585.go` too
  unless GR9c records otherwise.
- `bin/goi check` has no regression against its reference (GR7b's and GR8's, whichever is merged
  first); `bin/goi check --box`, the essential gate and the fixpoint pass.
- fib, sumSq and sieve are within GR3's criterion.
- The executable grows by less than 1 MB.
- `slices.Index` and the program's own generic functions are called from Arbace without a Go
  wrapper.

**Effort: 9-13 days.** The largest risk is the representation change of GR9b, since about 90
places in goi name proxies (`go/arbace/goi/method.ae` alone 50). X9 retired the riskiest
mechanism: descriptors and stubs working with compiled code, the collector and both
architectures.

**Order.** GR9 builds on GR8's table, since GR8 lists packages and reflect's targeted entries.
GR9a and GR9d are independent and can be built in parallel. GR9b needs GR9a.

## 7. Proposals for the user (GC65-GC74)

- **GC65 The route to runtime types of interpreted named types.**
  - (a) A file added to package `reflect` by the build's overlay (§2.2 A, §3.1).
  - (b) The same outside reflect, through `go:linkname` to `reflect.addReflectOff` and copied
    `internal/abi` layouts (reflectx's route).
  - (c) A type descriptor module goi writes (goloader's).
  - (d) Not built: the eleven programs stay known.

  *Recommended: (a)*: the runtime already resolves offsets made at run time, the overlay already
  exists, and the layouts are compile-checked.
- **GC66 The code behind methods' `Ifn`.**
  - (a) Typed Go stubs per method signature and slot, generated by the table for the listed
    interfaces' method signatures and the common ones; a panic stub for other signatures (§2.3 M1).
  - (b) Also reflectx-style assembly trampolines into `reflect.callReflect` for every other
    signature, with the runtime overlay's frame rule extended (M2).
  - (c) Trampolines only.
  - (d) A compiler patch so itabs hold closures (M3).

  *Recommended: (a)*, and (b) only if the check finds compiled code calling interpreted methods of
  other signatures.
- **GC67 Slots per family.**
  - (a) Counts in `gotable.edn`: 256 for `func() string` and `func() error`, 16 for each other
    family (about 325 KB); when one is exhausted, the method's stub panics with goi's message and
    the REPL warns at the type's definition.
  - (b) When exhausted, fall back to the proxies of today for that type.
  - (c) One large uniform count (1,024 each: about 14 MB).

  *Recommended: (a)*.
- **GC68 Generations and method sets.**
  - (a) A new generation when the underlying type or the method set's names or signatures change; a
    body redefined with its signature keeps it, its slot late-binding (§3.7; S8 amended).
  - (b) Spare room in each type's method array, methods added in place (cached itabs, failed ones
    included, would go stale).

  *Recommended: (a)*.
- **GC69 Proxies and wrappers after GS8.**
  - (a) Removed: every interface content is the value itself (§3.3).
  - (b) Kept as a fallback for types whose methods lack stubs.

  *Recommended: (a)*: one representation, and the panic stub names the rare gap.
- **GC70 Recursive types.**
  - (a) A type's shape as its layout before its underlying type, only where a constructor asks for
    its layout early (through a map), and GR7a's shaped Go-memory representation removed (§3.4).
  - (b) GR7a's shapes kept as they are, the named descriptors over them.

  *Recommended: (a)*: one RT per type, `reshape` gone.
- **GC71 Interpreted interface types.**
  - (a) RT `any` in Go memory as today; `reflect`'s view of an interface-typed field shows
    `interface {}`, a known difference no program of the check meets.
  - (b) Named interface descriptors of their own (`InterfaceType` with methods, reflectx's
    `NamedInterfaceOf`), values in such fields converted by `getitab`.

  *Recommended: (a)* at GR9, (b) when a program needs it.
- **GC72 Arbace values as operands of inference.**
  - (a) Arbace scalars as untyped constants of their kinds, Go values by their types, fns and
    collections not inferring (§4.3): Go's rule answers `int` or `int64`.
  - (b) Arbace scalars typed by default (`Long` as `int64`, `Double` as `float64`).
  - (c) Only Go values infer: a call whose type parameters only Arbace scalars would fix is an
    error asking for `go/inst`.

  *Recommended: (a)*: the same answer a Go call with literals gets.
- **GC73 Explicit type arguments from Arbace.**
  - (a) `(go/inst f T...)`, an instance's `GoFn` or `GoType`, partial allowed (§4.5).
  - (b) Metadata on the call (`^{:go/targs [(slice int64)]}`).
  - (c) Typed scalars from Arbace (`(go/typed int64 3)`, a Go value), against §5.6's rule that a Go
    scalar in Arbace is a `Long`.

  *Recommended: (a)*: Go forms' own `inst`, and a value one can `def`.
- **GC74 Caching instances of generic vars.**
  - (a) Per var, an inline entry and a map by the arguments' type keys, the instances shared with
    the builder's `instance-cell` (§4.4).
  - (b) No cache: `infer` at every call.

  *Recommended: (a)*.

## 8. Sources

- go1.27.1, TamaGo's toolchain `/root/tamago-go/src`:
  - `internal/abi/type.go`: `Type`, `UncommonType`, `Method`, `Uncommon`, `Name`, the `GCData`
    comment;
  - `internal/abi/map.go`: `MapType`;
  - `runtime/type.go`: `reflectOffs`, `resolveNameOff`, `resolveTypeOff`, `textOff`,
    `typelinksinit`, `typesEqual`;
  - `runtime/runtime1.go`: `reflect_addReflectOff`, `reflect_resolve*`;
  - `runtime/iface.go`: `getitab`, `itabInit`;
  - `runtime/stkframe.go`: `argMapInternal`;
  - `reflect/type.go`: `addReflectOff` and its linkname comment, `resolveReflect*`, `StructOf`,
    `ptrTo`, `typesByString`, `String`, `Name`, `PkgPath`, `Method`;
  - `reflect/value.go`: `methodReceiver`;
  - `cmd/compile/internal/types/fmt.go`: `tconv`, `SplitVargenSuffix`;
  - `cmd/compile/internal/noder/reader.go`: `mangle`;
  - `$GOROOT/test`: the programs of §1.1.
- reflectx v1.8.3 (Apache-2.0; studied, not vendored), `github.com/goplus/reflectx`, origin
  `683669fe520773240b3a1dfa7ef1a9fe0a209af8`, from the module proxy's zip:
  - `README.md`, `rtype.go`, `reflectx.go`, `name.go`, `methodof_icall.go`;
  - `icall/icall4096/icall_regabi.go`, `icall_regabi_go127_amd64.s`;
  - `cmd/iface_patch/README.md`.
- ixgo v1.2.3, `github.com/goplus/ixgo`, origin `a761e605548e65516bf99e374690456090771c76`
  (its version only; its use of reflectx as the REPL survey records).
- yaegi v0.16.1, the host's module cache: `README.md` ("Limitations"), `interp/type.go`.
- Arbace:
  - [GOI-SPEC.md](GOI-SPEC.md): §5.2, §5.3, §5.6, §7.1, §9, §13, and "As built" GR1-GR7a;
  - [GO-FORMS-REPL-SURVEY.md](../GO-FORMS-REPL-SURVEY.md): §2.2, §3, G8;
  - [BUILD.md](BUILD.md): amendment B5, the overlay;
  - `overlay/go/runtime.ae`, `go/arbace/goi/types.ae` (`TNamed`, `TSetUnder`),
    `arbace/go/build.ae` (`instance-cell`, `inst-type-name`), `arbace/go/repl.ae` (`define!`),
    `arbace/go/check/infer.ae` (`infer`);
  - `test/goi/check-known.edn`;
  - `target/arbace-go/gotable` (the table's wrappers, linux/amd64).
- Go issues: golang/go#16522 (NamedOf), #4146 (MakeInterface), #15924 and #38783 (StructOf's
  embedded methods), #67401 (linkname's "hall of shame").

## Appendix A: the experiments

Scratch in `.tmp/goi-gr9-survey/` (not tracked). `x9/env.sh` sets up the build: TamaGo's
go1.27.1, `CGO_ENABLED=0`, `GOTOOLCHAIN=local`, a build cache of its own, `GOPROXY=off`. Builds
ran through `.tmp/heavy`.

**X9: named types at run time** (`x9/overlay/zz_arbace_named.go`, Appendix B; `x9/overlay.json`
adds it to `$GOROOT/src/reflect`; `x9/proto/main.go`; `go build -overlay ../overlay.json`; arm64
by `GOARCH=arm64` and `qemu-aarch64`). The stubs are hand-written per family, as the table would
generate them (`slotS`, `s0`...). Results, identical on both architectures (`x9/run-amd64.txt`,
`x9/run-arm64.txt`):

| check | result |
|---|---|
| `type Celsius float64` with a value method `String`: `%T %v %s %#v` | `main.Celsius 36.6°C 36.6°C 36.6` (gc's `%#v` of a float kind is the number) |
| its `Name`, `PkgPath`, `Kind`, `String`; `PointerTo(C)` is `*C`'s descriptor; `%T` of `*Celsius`; `Implements(fmt.Stringer)`; `Println` of a `*Celsius` | `Celsius main float64 main.Celsius`; true; `*main.Celsius`; true; `36.6°C` |
| a `Celsius` asserted to `float64`; converted to `float64` | fails, as gc; `36.6` |
| `type RWS struct{}` with pointer methods `Read`, `Write`, `Seek`, `String`: asserted to `io.ReadWriteSeeker` by compiled code; `io.ReadAll` through the interpreted `Read` | true; `edcba` |
| `%[1]T %[1]v` of an `io.Reader` holding it; `%#v` | `*main.RWS rws`; `&main.RWS{}` |
| `RWS` (not `*RWS`) is not a `fmt.Stringer` | false |
| `p1.S` (`struct{X, Y int}`) `AssignableTo` the unnamed struct, and back; `Set` from it, `%T %+v` | `true true`; `p1.S {X:1 Y:2}` |
| `S2 struct{S1}`: `Field(0).Anonymous`, `FieldByName("S1")`; `%+v %#v` | `S1 true true true`; `{S1:{i:0}} main.S2{S1:main.S1{i:0}}` |
| issue77779's `render` of `Container{ZeroSize; Data *Data}` and of a pointer to it; the promoted `Render` | each calls the interpreted `Render` (12) |
| `MakeFunc` of `type ft func() *int`: `%T`, `NumIn`, `NumOut`, `Out(0)`; asserted to `func() *int`; 1,000 calls while a goroutine runs 200 collections | `main.ft 0 1 *int`; fails, as gc; no fault |
| `map[any]int` keyed by `main.T` (a slice field); `z == z`; asserted to `fmt.Stringer` | `hash of unhashable type main.T`; `comparing uncomparable type main.T`; `interface conversion: main.T is not fmt.Stringer: missing method String` |
| 200,000 `main.Box` (a pointer, a slice, a map) and a 200,000-node list of `main.Node` (`Next *Node`, built before `Node` is complete), five collections with churn | all intact (`0 19999900000`); `%T %+v` `main.Node {Next:<nil> V:0}`; `Next`'s type `*main.Node` |
| `type M map[string]M` (its shape `unsafe.Pointer` first); `T{M map[string]U; N int}`, `U{T T; P *int}` (`T`'s shape first), 50,000 map entries, two collections | `main.M map[a:map[]] main.M`; sizes 16 and 24, all intact |
| two generations of `main.P`: types equal, values equal, map keys, `%T` | `false false 2 main.P main.P` |
| `main.F[main.foo]`: `%T`, `Name` | `main.F[main.foo] F[main.foo]` |
| `json.Marshal` of a `p1.S`; of `[]any{J}` with an interpreted `MarshalJSON` | `{"X":1,"Y":2}`; `["J7"]` |
| `sort.Sort` over `main.IntSlice` (`Len`, `Less`, `Swap` by stubs) | sorted |
| `Ifn` replaced after an itab of the type exists | the itab keeps the old code (§3.7) |
| the costs | §5 |

**X9's size** (`x9/size/gen.py K`: 10 signature families, K stubs each, as in X9; `go build
-trimpath -ldflags=-s`): K = 0: 1,302,688 bytes; K = 100 (1,000 stubs): 1,527,968; K = 1,000
(10,000 stubs): 3,567,776. About 225 bytes a stub.

**X10: generic code from Arbace today** (`x10/t.ae`, run by the stage 0: `arbace t.ae`). A var of
a Go function calling `slices.Index` on a Go `[]int` costs 1,493 ns a call, the same loop written
for `[]int` 1,508 ns. `go/eval` of `slices.Index(...)` costs 144 ms a call, of the non-generic one
139 ms: `go/eval` checks the REPL's package each time.

## Appendix B: the prototype's overlay file

`x9/overlay/zz_arbace_named.go`, added to package `reflect` by `go build -overlay`. Its two maps
are unsynchronized (a prototype); §3.1 lists what the overlay of GR9a adds.

```go
// GR9 survey prototype (X9): named types made at run time, inside package reflect
// (an overlay file, as overlay/go patches the runtime). Not Arbace's code: the
// survey's evidence for doc/go/GR9-SURVEY.md.

package reflect

import (
	"internal/abi"
	"sort"
	"unsafe"
)

// ArbaceMethod is one method of a type made by ArbaceNamed.
type ArbaceMethod struct {
	Name     string
	Exported bool
	Type     Type           // the method's type without its receiver
	Ifn      unsafe.Pointer // code called through an itab and by Value.Method (receiver: one word)
	Tfn      unsafe.Pointer // code of Type.Method's Func (receiver by value); nil: unreachable
}

func arbaceHeader(k Kind) Type {
	switch k {
	case Struct:
		return TypeFor[structType]()
	case Func:
		return TypeFor[funcType]()
	case Pointer:
		return TypeFor[ptrType]()
	case Slice:
		return TypeFor[sliceType]()
	case Array:
		return TypeFor[arrayType]()
	case Chan:
		return TypeFor[chanType]()
	case Map:
		return TypeFor[abi.MapType]()
	case Interface:
		return TypeFor[interfaceType]()
	default:
		return TypeFor[abi.Type]()
	}
}

// arbaceAlloc allocates a type descriptor of kind k followed by its uncommon type,
// nparams parameter types (functions) and nm methods, at a Go type the collector scans.
func arbaceAlloc(k Kind, nparams, nm int) (*abi.Type, *uncommonType, []abi.Method) {
	fs := []StructField{
		{Name: "H", Type: arbaceHeader(k)},
		{Name: "U", Type: TypeFor[uncommonType]()},
	}
	if nparams > 0 {
		fs = append(fs, StructField{Name: "P", Type: ArrayOf(nparams, TypeFor[*abi.Type]())})
	}
	if nm > 0 {
		fs = append(fs, StructField{Name: "M", Type: ArrayOf(nm, TypeFor[abi.Method]())})
	}
	v := New(StructOf(fs)).Elem()
	t := (*abi.Type)(v.Field(0).Addr().UnsafePointer())
	ut := (*uncommonType)(v.Field(1).Addr().UnsafePointer())
	ut.Moff = uint32(unsafe.Sizeof(uncommonType{})) + uint32(nparams)*uint32(unsafe.Sizeof(uintptr(0)))
	var ms []abi.Method
	if nm > 0 {
		ms = unsafe.Slice((*abi.Method)(v.Field(len(fs)-1).Addr().UnsafePointer()), nm)
	}
	t.Kind_ = abi.Kind(k)
	t.TFlag = abi.TFlagUncommon
	resolveReflectType(t) // pins it: reflectOffs holds the pointer
	return t, ut, ms
}

var arbaceCounter uint32

// arbaceUncommon is each type's uncommon type, by its descriptor: found by the kind it was
// allocated for, whatever kind its header has while its layout is its shape's (ArbaceSetLayout).
var arbaceUncommon = map[*abi.Type]*uncommonType{}

// ArbaceNamed makes the named type str (gc's runtime name, "main.T") of package
// pkgPath and kind k, and its pointer type, with room for nm and npm methods;
// nparams is the underlying function type's parameter and result count. The
// underlying type is given by ArbaceSetUnderlying, the methods by ArbaceSetMethods.
func ArbaceNamed(pkgPath, str string, k Kind, nparams, nm, npm int) (Type, Type) {
	t, ut, _ := arbaceAlloc(k, nparams, nm)
	arbaceCounter++
	arbaceUncommon[t] = ut
	t.TFlag |= abi.TFlagNamed | abi.TFlagExtraStar
	t.Str = resolveReflectName(newName("*"+str, "", false, false))
	t.Hash = fnv1(fnv1(0, []byte(str)...), byte(arbaceCounter>>24), byte(arbaceCounter>>16), byte(arbaceCounter>>8), byte(arbaceCounter))
	ut.PkgPath = resolveReflectName(newName(pkgPath, "", false, false))

	var iptr any = (*unsafe.Pointer)(nil)
	proto := *(**ptrType)(unsafe.Pointer(&iptr))
	p, put, _ := arbaceAlloc(Pointer, 0, npm)
	pt := (*ptrType)(unsafe.Pointer(p))
	moff := put.Moff
	pt.PtrType = proto.PtrType
	pt.TFlag = proto.TFlag | abi.TFlagUncommon
	pt.Str = resolveReflectName(newName("*"+str, "", false, false))
	pt.Hash = fnv1(t.Hash, '*')
	pt.PtrToThis = 0
	pt.Elem = t
	*put = uncommonType{PkgPath: ut.PkgPath, Moff: moff}
	t.PtrToThis = resolveReflectType(p)
	return toType(t), toType(p)
}

// ArbaceSetUnderlying gives T the layout, kind data and operations of under;
// embedded lists the struct fields to mark as embedded.
func ArbaceSetUnderlying(T, under Type, embedded []int) {
	t := T.common()
	u := under.common()
	nu := arbaceUncommon[t] // not t.Uncommon(): the header's kind may be its shape's
	str, hash, ptt, ut := t.Str, t.Hash, t.PtrToThis, *nu
	switch Kind(u.Kind()) {
	case Struct:
		st := (*structType)(unsafe.Pointer(t))
		*st = *(*structType)(unsafe.Pointer(u))
		if len(embedded) > 0 {
			fs := append([]abi.StructField(nil), st.Fields...)
			for _, i := range embedded {
				f := &fs[i]
				f.Name = newName(f.Name.Name(), f.Name.Tag(), f.Name.IsExported(), true)
			}
			st.Fields = fs
		}
	case Func:
		ft := (*funcType)(unsafe.Pointer(t))
		uf := (*funcType)(unsafe.Pointer(u))
		*ft = *uf
		ps := unsafe.Slice((**abi.Type)(unsafe.Add(unsafe.Pointer(nu), unsafe.Sizeof(uncommonType{}))), len(uf.InSlice())+len(uf.OutSlice()))
		n := copy(ps, uf.InSlice())
		copy(ps[n:], uf.OutSlice())
	case Pointer:
		*(*ptrType)(unsafe.Pointer(t)) = *(*ptrType)(unsafe.Pointer(u))
	case Slice:
		*(*sliceType)(unsafe.Pointer(t)) = *(*sliceType)(unsafe.Pointer(u))
	case Array:
		*(*arrayType)(unsafe.Pointer(t)) = *(*arrayType)(unsafe.Pointer(u))
	case Chan:
		*(*chanType)(unsafe.Pointer(t)) = *(*chanType)(unsafe.Pointer(u))
	case Map:
		*(*abi.MapType)(unsafe.Pointer(t)) = *(*abi.MapType)(unsafe.Pointer(u))
	case Interface:
		*(*interfaceType)(unsafe.Pointer(t)) = *(*interfaceType)(unsafe.Pointer(u))
	default:
		*t = *u
	}
	t.Str, t.Hash, t.PtrToThis = str, hash, ptt
	t.TFlag = u.TFlag&^(abi.TFlagUncommon|abi.TFlagNamed|abi.TFlagExtraStar) | abi.TFlagUncommon | abi.TFlagNamed | abi.TFlagExtraStar
	*nu = ut
}

// ArbaceSetMethods fills T's method table (its room given to ArbaceNamed), exported
// methods first, each group by name, as gc sorts them.
func ArbaceSetMethods(T Type, ms []ArbaceMethod) {
	t := T.common()
	ut := t.Uncommon()
	ms = append([]ArbaceMethod(nil), ms...)
	sort.Slice(ms, func(i, j int) bool {
		if ms[i].Exported != ms[j].Exported {
			return ms[i].Exported
		}
		return ms[i].Name < ms[j].Name
	})
	dst := unsafe.Slice((*abi.Method)(unsafe.Add(unsafe.Pointer(ut), ut.Moff)), len(ms))
	x := 0
	for i, m := range ms {
		tfn := abi.TextOff(-1)
		if m.Tfn != nil {
			tfn = abi.TextOff(resolveReflectText(m.Tfn))
		}
		dst[i] = abi.Method{
			Name: abi.NameOff(resolveReflectName(newName(m.Name, "", m.Exported, false))),
			Mtyp: abi.TypeOff(resolveReflectType(m.Type.common())),
			Ifn:  abi.TextOff(resolveReflectText(m.Ifn)),
			Tfn:  tfn,
		}
		if m.Exported {
			x++
		}
	}
	ut.Mcount = uint16(len(ms))
	ut.Xcount = uint16(x)
}

// ArbaceSetLayout gives T, before its underlying type, the header of shape, a type of its
// layout and pointer words (GR7a's shape): T's size, alignment, pointer mask and kind data
// while constructors (MapOf, StructOf, ArrayOf) of types containing T are made. T has no
// uncommon type meanwhile; ArbaceSetUnderlying gives it back.
func ArbaceSetLayout(T, shape Type) {
	t := T.common()
	u := shape.common()
	str, hash, ptt := t.Str, t.Hash, t.PtrToThis
	switch Kind(u.Kind()) {
	case Struct:
		*(*structType)(unsafe.Pointer(t)) = *(*structType)(unsafe.Pointer(u))
	case Array:
		*(*arrayType)(unsafe.Pointer(t)) = *(*arrayType)(unsafe.Pointer(u))
	default:
		*t = *u
	}
	t.Str, t.Hash, t.PtrToThis = str, hash, ptt
	t.TFlag = u.TFlag&^(abi.TFlagUncommon|abi.TFlagNamed|abi.TFlagExtraStar) | abi.TFlagExtraStar
}
```
