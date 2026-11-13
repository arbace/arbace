# WebAssembly as a crop: survey and plan

Status: survey and plan, 2026-11-13, at the user's request: WebAssembly a crop of its own, read, eval, AOT and print, as the x86-64
and arm64 assembly crops are (doc/asm/AMD64-SURVEY.md, doc/asm/ARM64-SURVEY.md: the model), and this time with a **minimal HTML and
JavaScript shim** for Wasm in the browser, the way the single-page WebGPU games run (a canvas, `requestAnimationFrame`, input events,
WebGPU through a thin bridge, assets by `fetch`, no framework). Nothing here is adopted: only this file, a journal entry and an AGENDA
line were written; no code under `arbace/`, `go/`, `bin/`, `tools/` or `test/` changed. The decisions are the user's, as proposals
WAS1-WAS26 (§15); the milestones are WA1-WA12 (§14), with two optional ones.

Base: branch `wasm-survey` from `mac/next` `ca3c7aff8`. The work is reading (the two assembly surveys whole, doc/CROPS-SCORE.md,
`test/reap/cells.edn`, `test/gate/crops/asm.edn`, `test/crops/crops.edn`, CLAUDE.md's crop sections, doc/go/GOI-SPEC.md's opening,
`tamago/cmd/`'s `cmd/compile/internal/wasm`, `cmd/internal/obj/wasm` and `cmd/link/internal/wasm`, TamaGo's go1.27.1 tree's `lib/wasm`,
`misc/wasm`, `runtime/*wasm*`, `runtime/*js*`, `syscall/js`, the seed's module) and light probes of seconds each (§3) in the worktree's
`.tmp/wasm/` (Appendix A), during night 13's rerun (one run at a time, each under 0.3 GiB). No Arbace executable was built, no lock taken,
nothing installed, nothing downloaded; the testsuite's counts come from the anonymous read-only Sourcegraph search (memory:
sourcegraph-public-search) and are marked **to verify** on the fetched pin.

Words used throughout:

- **Wasm forms**: a Wasm module as Arbace forms (§4.2): the spec's abstract syntax (types, imports, functions, tables, memories,
  globals, exports, start, elements, data, tags, custom sections), instructions as a sequence with structured blocks, whichever format
  it was read from.
- **The two formats**: the binary format (`.wasm`, the spec's chapter 5) and the text format (WAT, `.wat`, chapter 6). **WAST**: the
  testsuite's script format, WAT plus commands (`module`, `register`, `invoke`, `assert_return`, ...).
- **repr/wasm**: the bridge between both formats and the forms (`bin/repr-wasm read|print|roundtrip|check`), as repr/asm and repr/llvm.
- **wai**: the evaluator, an interpreter of Wasm modules in Go forms, a light driver as x86i and a64i are (the name is WAS7's).
- **w2g**: AOT, Wasm → Go forms, built by TamaGo's go (WAS10).
- **The shim**: one `index.html` and one small JavaScript file that load a Wasm module in a browser and give it the host it imports
  (§9).
- **Own verdict**: a test's verdict that no toolchain made (the user's ruling of 2026-11-07): here the testsuite's assertions, a Go test's
  `.out` file or header, a program's exit status.

Contents: Summary · 1 What parity means · 2 The host and prior art · 3 What the probes showed · 4 Read · 5 Print · 6 eval: wai ·
7 AOT: w2g · 8 Arbace to Wasm · 9 The browser side · 10 The judges · 11 Independence and the oracle tier · 12 The corpora · 13 Layout,
row, gate and budget · 14 Plan (WA1-WA12) · 15 Proposals (WAS1-WAS26) · 16 What to decide first · 17 Risks · 18 Sources · Appendix A.

## Summary

- **Wasm is an intermediate crop with its own judge written in.** Like LLVM IR and the two assemblies it is a target more than a source
  language: Go, clang, Rust and the GC languages emit it. Unlike them its specification ships its own conformance suite as scripts of
  assertions (`assert_return`, `assert_trap`, `assert_invalid`, `assert_malformed`, ...): **every cell can be judged by own verdicts**
  from day one, with no reference engine at check time (§10).
- **Arbace's executable is already one `GOOS` away from the browser.** It is a Go program built by TamaGo's go1.27.1, which builds
  `GOOS=js GOARCH=wasm` and `GOOS=wasip1 GOARCH=wasm` out of the box (P1). The seed's module resolves for both with no missing package
  (P4); what is missing is a third variant of jrt's host layer (files, processes, randomness, network, the store) beside the linux and
  box (tamago) ones, about 750 lines each there (P4, §8.2), and goi's table's parts for the two GOOS. The REPL starts in 0.25 s and
  158 MB natively (P5): it fits a tab. The executable is 207 MB; a wasm build would be about 215-220 MB, about 60-65 MB gzip (estimates
  from P1's ratios, to measure, WA9).
- **Go's own toolchain is the crop's best corpus producer and is not a host toolchain.** TamaGo's go is the seed's (doc/CROPS-SCORE.md:
  not counted), so `$GOROOT/test`'s programs built for `wasip1` and run by wai, judged by their `.out` files and gc's run as goi's check
  judges them, keep eval and AOT at independence 1 (WAS11). Its `cmd/compile/internal/wasm`, `cmd/internal/obj/wasm` and
  `cmd/link/internal/wasm` are already Go forms in `tamago/cmd/` (4,184 lines).
- **The engine, wai, is Arbace's own** (Go forms, hand-written after the spec's semantics, wasmi's and wazero's designs as references,
  WAS6), with three hosts: the testsuite's `spectest` module, WASI preview 1 (the 38 functions Go's `wasip1` port imports, P3), and a
  **headless `gojs` world** (the 24 imports Go's `wasm_exec.js` provides, P3) that lets Go's `js/wasm` programs, Arbace's included, run
  and be judged without a browser.
- **The browser part is small and honest about its judge.** The shim is Arbace's own `index.html` and one JavaScript file derived from Go
  1.27.1's `wasm_exec.js` (BSD-3), with an asynchronous file system (memory plus `fetch`), a frame loop, input, and a WebGPU bridge whose
  per-frame calls go as one command buffer in linear memory (WAS15, WAS16). At check time the Go side's command stream is judged under
  wai's headless world by the demo's own stated digests; a real browser only at the oracle tier (headless Chromium on a software WebGPU
  adapter, recorded frames) and by hand on the Mac (WAS17). The browser is not a REAP cell.
- **Proposals in order** (WAS4): Wasm 2.0 first (MVP plus sign extension, saturating conversions, multi-value, bulk memory, reference
  types: exactly what Go and clang emit by default, P3, P6), then SIMD128, then 3.0's rest without GC (tail calls, exceptions, memory64,
  multiple memories, extended constants, typed function references), then GC, threads last.
- **Plan (§14): WA1-WA12, about 62 to 105 working days.** WA1 is small and useful at once: the binary format read and printed back byte
  for byte over modules TamaGo's go builds now (no fetch), every instruction Go emits decoded, the forms read again equal.
- **To fetch, with the user's OK** (§12): WebAssembly/testsuite at one commit (Apache-2.0, 293 `.wast`, about 56,500 assertion lines, a
  few MB: to verify) before WA2; the spec's reference interpreter (OCaml, Apache-2.0) optionally, to run under oi as a second judge
  (WAS19); the oracle tier's packages (wabt, wasmtime, nodejs, chromium) only when the user wants that tier.
- **To decide first (§16):** WAS1 (the row), WAS2 (the forms), WAS6 and WAS7 (the engine), WAS12 (Arbace to Wasm and its key window),
  WAS17 (the browser's judge), WAS21 (the fetches).

## 1. What parity means

The crops' rule for an intermediate language (doc/llvm/LLVM-CROP-SURVEY.md §1, the asm surveys' §1): the subject is the module itself,
any producer's; the reference is the specification's reading and execution of it, as the testsuite states them.

| cell | in Arbace (proposed) | corpus | own verdict | oracle tier (by hand, at a tag) |
|---|---|---|---|---|
| **read** | repr/wasm `read`: binary and WAT → Wasm forms; WAST scripts read; the decoder's and the validator's acceptance | the testsuite's modules (text, `binary`, `quote`); Go-built modules; clang's packs (oracle tier) | the forms read again equal; `assert_malformed` and `assert_invalid` rejected (their message a second level); text and binary of one module to the same forms | wabt's `wat2wasm`/`wasm-validate` accept or reject the same |
| **eval** | wai: instantiate, link, run; `spectest`, WASI p1, headless `gojs` | the testsuite's commands; `$GOROOT/test` built for `wasip1`; Go `js/wasm` programs | `assert_return` (NaN patterns), `assert_trap`, `assert_exhaustion`, `assert_exception`, `assert_unlinkable`, `assert_uninstantiable`; Go's `.out`; exit status | wasmtime, node, the spec's reference interpreter on the same |
| **AOT** | w2g: the module as Go forms, built by TamaGo's go, run | the same scripts (one program a script), the same Go programs | the same assertions and outputs | the same engines |
| **print** | repr/wasm `print`: canonical binary, canonical WAT; `:full` both | every read corpus | `:full` byte for byte (binary and text); canonical print read again equal; WAT → binary → forms equal (the cross-format judge) | `wat2wasm` of Arbace's WAT equal to Arbace's binary |

linux/amd64 for every check; wai and w2g are Go forms, so the same checks run on arm64 (the Mac VM) and, later, in the box.

**What differs from the assembly crops.** No CPU runs Wasm here, and no engine is on the host (P7): the testsuite's assertions replace
the CPU's run and the lockstep as the reference. The difficult parts move: the **validator** (typing with subtyping once GC is in), the
**determinism rules** (NaN payloads, relaxed SIMD's allowed results), and the **host interfaces** (WASI, `gojs`, the browser).

## 2. The host and prior art

### 2.1 The host

- TamaGo's go1.27.1 (`/root/tamago-go`): builds `js/wasm` and `wasip1/wasm` (P1); `lib/wasm/wasm_exec.js` (575 lines),
  `wasm_exec_node.js`, `go_js_wasm_exec`, `go_wasip1_wasm_exec` (they call node and wasmtime, which the host lacks); `misc/wasm/
  wasm_exec.html` (48 lines). Not counted as a host toolchain (doc/CROPS-SCORE.md).
- clang 23.1.5 with the `wasm32` and `wasm64` targets, `wasm-ld` (lld is installed now); `llc` (P6). LLVM IR's pin; a host toolchain
  (`:llvm`), the oracle tier's only.
- No Wasm engine, no Wasm tool, no browser (P7).
- The pinned llvm-project 23.1.5 tarball (`.tmp/llvm/`): `llvm/test/MC/WebAssembly` 106 files, `CodeGen/WebAssembly` 413,
  `lld/test/wasm` 310, `lib/Target/WebAssembly` 126 (P9).

### 2.2 Prior art (licences checked where on the host; else to verify)

| project | what | relevance | licence | use |
|---|---|---|---|---|
| **The WebAssembly specification** (`WebAssembly/spec`: the document, `interpreter/` the reference interpreter in OCaml, `test/core`) | the formal semantics, the validation algorithm (its appendix), the binary and text grammars; the interpreter is the testsuite's own reference | wai's and the validator's semantics, rule for rule; the reference interpreter runnable by oi (WAS19) | the interpreter and tests Apache-2.0; the document W3C's Software and Document licence (to verify) | reference; fetch for WAS19 |
| **WebAssembly/testsuite** | the spec's and the proposals' `.wast` scripts mirrored in one repo | the corpus (§12) | Apache-2.0 (its LICENSE, Sourcegraph, 2026-11-13) | test data, fetch |
| **Go's wasm port** (`cmd/compile/internal/wasm`, `cmd/internal/obj/wasm`, `cmd/link/internal/wasm`, `runtime/*_wasm*`, `*_js*`, `*_wasip1*`, `syscall/js`, `syscall/fs_js.go`, `lib/wasm/wasm_exec.js`) | Go's back end, assembler and linker for wasm; the runtime's ABI (`gojs`, WASI p1) | Arbace's own build (§8); the `gojs` ABI the shim implements (§9.2); a producer of the run corpus; the opcode table (`a.out.go`, `anames.go`) | BSD-3 (Go's, in LICENSE.md) | reuse (already forms in `tamago/cmd/`), the shim derived from `wasm_exec.js` |
| **wazero** (`tetratelabs/wazero`) | a Wasm runtime in pure Go, no cgo: decoder, validator, an interpreter and a compiler, WASI p1, the spectest harness | the closest prior art in Arbace's host language; its interpreter's IR and its WASI layer a reference for wai | Apache-2.0 (to verify) | reference; a conversion by repr/go is WAS6 (b) |
| **wasmi** (`wasmi-labs/wasmi`) | a Rust interpreter, a register-machine IR translated at load, the user's "wasmi-like" | wai's IR design (WA4, its second step) | MIT or Apache-2.0 (to verify) | reference |
| **wasm3**, **WAMR** (fast interpreter) | interpreters with threaded code | speed techniques | MIT, Apache-2.0 WITH LLVM-exception (to verify) | reference |
| **wabt** (`wat2wasm`, `wasm2wat`, `wasm-validate`, `wasm-interp`, `spectest-interp`, `wasm2c`) | the reference toolkit | the oracle tier's tools; `wasm2c` the shape of AOT's WAS10 (c) | Apache-2.0 (to verify) | run at the oracle tier |
| **wasm-tools** (Bytecode Alliance: `wasmparser`, `wasmprinter`, `wast`, `wasm-smith`) | parser, printer, the WAST reader, a fuzzer of valid modules | the printer's canonical text (what `wasm-tools print` writes); `wasm-smith` a generator of valid modules for WA12's controls | Apache-2.0 WITH LLVM-exception or MIT (to verify) | reference; oracle tier |
| **wasmtime** (Cranelift), **V8**, **SpiderMonkey**, **JavaScriptCore** | the production engines | the oracle tier's runs (wasmtime; node's V8) and the browser's | Apache-2.0 WITH LLVM-exception; BSD-3; MPL-2.0; LGPL-2/BSD (to verify) | run only |
| **Binaryen** (`wasm-opt`) | optimizer | not needed; out | Apache-2.0 | none |
| **WebGPU** (W3C's spec and `webgpu.idl`, gpuweb/gpuweb), **WGSL** | the browser's GPU API and shading language | the bridge (§9.3); the IDL generates the bindings in WAS16 (c) | W3C Software and Document licence (to verify) | reference, the IDL as data |
| **webgpu-headers** (`webgpu.h`), **Dawn**'s `emdawnwebgpu`, **wgpu** | the C API shape of WebGPU and its JS implementations for Wasm | the command buffer's operations named after `webgpu.h` (WAS16) | BSD-3, BSD-3, MIT or Apache-2.0 (to verify) | reference |
| **Ebitengine**, **cogentcore/webgpu**, **sokol** | Go and C game frameworks on the browser (frame loop by `requestAnimationFrame` through `js.FuncOf`, input, canvas sizing) | the shim's and the demo's structure | Apache-2.0, BSD-3, zlib (to verify) | reference |

### 2.3 What Arbace already has

- **Go's toolchain as forms** (`tamago/cmd/`, S9): the wasm back end, assembler and linker among the 105 packages (P3); Arbace can build
  Go to Wasm from its own forms of gc (the box's tool guests show the route), and so the crop's producer is Arbace's own toolchain.
- **The box's host layer** (`jrt/hostbox_tamago.go`, `netbox_tamago.go`, `spawn_tamago.go`): the second GOOS the program already
  supports; the js and wasip1 variants follow its shape (§8.2). The runtime's overlay's event hook (`arbace_setEventHook`,
  `arbace_eventPark` in `overlay/go/runtime/arbace_local.go`, the box's doorbell) is what a browser's event loop needs too.
- **The light-driver pattern** (x86i, a64i, oi's and si's drivers): Go forms built alone by TamaGo's go, out of every key
  (`arbace_crop_paths`), run by an Arbace script that judges their output. wai is one more.
- **The crops' checking frame**: GT9's fragments and the splice, the REAP readers (`:asm-rows`, `:asm-counts`), the negative controls
  (GTS81 (a)), the oracle records (doc/ORACLE-RECORDS-SURVEY.md), crops-score's three measures.
- **oi** (OCaml on Arbace) with repr/ocaml's Menhir port: the spec's reference interpreter is an OCaml program with a Menhir grammar,
  runnable by Arbace itself (WAS19), a cross-crop dividend no other crop has had.
- **goi's check** over `$GOROOT/test` (1,837 programs, gc's runs as the reference): the run corpus and its judge exist; WA5 builds the
  same programs for `wasip1`.

## 3. What the probes showed

All in `.tmp/wasm/` (Appendix A), seconds each, under 0.3 GiB.

- **P1, Go to Wasm**: a hello program with a goroutine, a channel and `time.Now` built by TamaGo's go (`CGO_ENABLED=0`, cold cache):
  `js/wasm` 2,532,430 bytes (11.0 s, 270 MB RSS), `wasip1/wasm` 2,503,267 (9.6 s), `linux/amd64` 2,407,795 (7.4 s). The wasm builds are
  1.04-1.05 times the native one; gzip -9 keeps 29% (730,611 bytes), xz -9 21% (542,048).
- **P2, the module's shape** (a 50-line section dumper, `probe/`): `js/wasm`: 20 imports, all from module `gojs`; 1,647 functions; code
  1,557,982 bytes, data 923,291, the `name` section 45 KB, `go:buildid` and `producers` custom sections; one memory, minimum 37 pages
  (2.3 MiB); 4 exports. `wasip1/wasm`: 16 imports from `wasi_snapshot_preview1`, 2 exports.
- **P3, Go's runtime ABI and features**: `wasm_exec.js` provides 24 `gojs` imports (8 `runtime.*`: `wasmExit`, `wasmWrite`,
  `resetMemoryDataView`, `nanotime1`, `walltime`, `scheduleTimeoutEvent`, `clearTimeoutEvent`, `getRandomData`; 16 `syscall/js.*`); Go's
  runtime and syscall import 38 WASI preview 1 functions (`fd_*`, `path_*`, `poll_oneoff`, `clock_time_get`, `random_get`,
  `sock_accept`, ...). `internal/buildcfg` treats `GOWASM=satconv,signext` as "always enabled"; `memmove_wasm.s` and `memclr_wasm.s` use
  `memory.copy` and `memory.fill` (bulk memory). Go 1.27's `cmd/compile/internal/wasm/simdssa.go` (generated) emits SIMD128 under the
  SIMD experiment. So Arbace's own build needs Wasm 2.0's sign extension, saturating conversions and bulk memory: WAS4's first set.
- **P4, Arbace's program for Wasm**: the seed's module (`arbace-seed-go.tar.xz`, 2,300 files, 157 MB of Go) under `go list -e -deps`:
  `js/wasm` and `wasip1/wasm` resolve 397 packages with no error (`linux/amd64`: 404; the differences `os/signal`, `os/user`,
  `internal/runtime/cgroup`, `x/sys/cpu` and three crypto internals out, `syscall/js` in). 30 files carry build constraints: 12 `linux`
  (jrt's `hostfs`, `hostproc`, `hostrand`, `net`, `resources`, `spawn`, `store`, 752 lines, and goi's table's `invokers`, `net`,
  `os_signal`, `runtime` parts), 8 `tamago` (`hostbox`, `netbox`, `spawn`, 771 lines, and the table's), 8 by GOOS and arch, `!linux` and
  `!tamago` one each. On js and wasip1 the linux files drop out and nothing replaces them: the port is a third variant (§8.2). Not
  compiled: the whole program's build is a night's (about 8 minutes, several GiB).
- **P5, the REPL's size**: stage 0's `arbace -e '(println (+ 1 2) (count (all-ns)))'` in 0.25 s and 158 MB RSS; `(f 27)`, a naive
  Fibonacci, 154 ms evaluated. The executable is 206,876,832 bytes (`-s -w`); by P1's ratios a wasm build would be about 215-220 MB, 60-65
  MB gzip, 45 MB xz (to measure: WA9).
- **P6, clang to Wasm**: `clang --target=wasm32 -O2 -nostdlib` and `wasm-ld --no-entry`: a 333-byte module with a `target_features`
  custom section; `clang -S` writes LLVM's wasm assembly (`.functype fib (i32) -> (i32)`, `local.get 0`, `block`, `br_if 0`), a
  syntax of its own, not WAT (WAS20).
- **P7, the host's engines**: none of node, deno, bun, wasmtime, wasmer, wasm3, wat2wasm (wabt), wasm-tools, wasm-opt, chromium,
  firefox. Every oracle needs an install (§12).
- **P8, the testsuite** (Sourcegraph, `github.com/WebAssembly/testsuite`, 2026-11-13): 293 `.wast` files; 258 at the root (59
  `simd_*`, 4 `relaxed_*`, about 26 memory64, about 20 GC, 4 exceptions, 3 `return_call*`, 18 bulk memory and tables), `proposals/`
  (custom-descriptors 14, custom-page-sizes 5, threads 4, extended-name-section 2, compact-import-section 2, wide-arithmetic 1),
  `legacy/` (4, the first exceptions proposal), `custom/` (3, annotations); about 56,500 lines with `(assert_`. To verify on the pin.
- **P9, LLVM's Wasm tests** (the pinned tarball's listing): `MC/WebAssembly` 106 (LLVM's assembly syntax, P6), `CodeGen/WebAssembly`
  413 (`llc` tests: IR in, assembly out; LLVM IR's crop's business), `lld/test/wasm` 310 (the linker's: object files with the `linking`
  and `reloc.*` custom sections).

## 4. Read

### 4.1 The formats

| format | grammar | corpus | notes |
|---|---|---|---|
| **binary** | sections in order (custom ones anywhere), LEB128 integers (unsigned and signed, any width up to the bound: a producer may pad), UTF-8 names, the instruction encoding (one-byte opcodes, prefixes `0xFC` (saturating, bulk, table), `0xFD` (SIMD), `0xFB` (GC), `0xFE` (threads)) | Go-built modules (P2), the testsuite's `(module binary ...)` and `assert_malformed` binaries, clang's packs | decoding is acceptance: the spec's binary grammar, the malformed tests' first level |
| **WAT** | S-expressions; identifiers `$x`; abbreviations (inline imports and exports, inline type uses, folded instructions, `(param i32 i32)`); numbers (`0x`, `_` separators, hex floats, `nan:0x...`, `inf`); strings with `\hh` and `\u{...}` escapes; comments `;;` and `(; ;)` nested; annotations `(@name ...)` | the testsuite's text modules, `(module quote ...)` | not Clojure-readable as is (`(;`, `\00`, `nan:0x1`, `1_000`): its own lexer, the spec's lexical grammar |
| **WAST** | WAT modules plus the commands (`module` with `$name`, `binary`, `quote`, `definition`, `instance`; `register`, `invoke`, `get`, the asserts, `thread` and `wait` for threads) | the testsuite | the corpus's unit is the command; each assert one case |

LLVM's wasm assembly (P6, `MC/WebAssembly`) is a third text, LLVM's, with its object files' `linking` and `reloc.*` sections; it is
repr/asm's kind of syntax and waits (WAS20).

### 4.2 The forms (to settle in doc/wasm/SPEC.md, WA1)

Proposed (WAS2 (a)), as repr/asm's are the machine level:

```clojure
(wasm/module {:name $m}
  (wasm/type 0 (func [i32] [i32]))
  (wasm/import "wasi_snapshot_preview1" "fd_write" (func 0 {:type 1}))
  (wasm/memory 0 {:min 37} {:export "mem"})
  (wasm/func 1 {:type 0 :name $fib :locals [i32]}
    (local.get 0) (i32.const 2) (i32.lt_s)
    (if {:bt [i32]}
      [(local.get 0)]
      [(local.get 0) (i32.const 1) (i32.sub) (call 1)
       (local.get 0) (i32.const 2) (i32.sub) (call 1) (i32.add)]))
  (wasm/export "fib" (func 1))
  (wasm/data 0 {:memory 0 :offset [(i32.const 1024)]} "hello\n")
  (wasm/custom "producers" #bytes "..."))
```

- **The abstract syntax**, the spec's: each field kind with its index explicit, instructions by the spec's names (`i32.lt_s`,
  `memory.copy`, `v128.load8x8_s`), immediates as data (`{:offset 8 :align 2 :memory 0}`), blocks as nested vectors. One vocabulary for
  text and binary, so a print in the other format is free and is a judge (§5).
- **Names**: the text's `$x` and the binary's `name` section map onto the same `:name` keys; an unnamed index stays an index.
- **The spelling** in `:full`'s metadata, not in the forms: the text's folding, abbreviations, numeral spellings, comments, layout; the
  binary's LEB widths, section order and sizes, the custom sections' places, a `datacount` section's presence. Canonical prints come
  from the forms alone.
- **WAST** as `wast/` forms over the modules (`(wast/assert-return (wast/invoke $m "f" [(i32.const 1)]) [(i32.const 2)])`), with
  `nan:canonical` and `nan:arithmetic` as result patterns.

### 4.3 The reader's acceptance

Three levels, in order, each the testsuite's own: decoding (the binary grammar: `assert_malformed` on binaries: "unexpected end",
"integer too large", "section size mismatch"), parsing (the text grammar: `assert_malformed` on `quote` modules: "unknown operator",
"unexpected token"), and validation (`assert_invalid`: the typing rules, "type mismatch", "unknown local"). Validation is the spec's
algorithm (its appendix: an operand stack of value types with unknown for the unreachable, a control stack), in Arbace
(`arbace.wasm.valid`), and its rejection is read's acceptance (WAS9), as the asm crops' matching is. The testsuite's messages are the
reference interpreter's; the spec lets an engine word them otherwise, so a rejection passes the level and the message is a second,
`:beside` level judged by prefix (the reference runner's rule, WAS8).

## 5. Print

- **Canonical binary**: minimal LEBs, sections in the spec's order, no custom section moved; as the spec's encoder and wabt write.
- **Canonical WAT**: one instruction a line, flat (unfolded), indices with names where known, as `wasm-tools print` and `wasm2wat`
  write (the two differ in details: WAS2's spec names the house style).
- **`:full`**: the binary byte for byte (padded LEBs, the producer's section layout: Go's and LLVM's objects pad), the text byte for
  byte (comments, folding, spellings). Every read corpus's `:full` is a print check.
- **Cross-format**: WAT → forms → binary → forms equal, and binary → forms → WAT → forms equal. Cheap and strict: it catches a reader
  that keeps the text's shape in the forms.

## 6. eval: wai

### 6.1 The candidates (WAS6)

- (a) **An interpreter of Arbace's own in Go forms** (`go/arbace/wai`), a light driver built alone by TamaGo's go, out of every key: the
  module decoded and validated once, each function translated at load into a compact internal code (branch targets resolved, the value
  stack as 64-bit slots, `v128` as two), then a loop over it; later a register-machine translation as wasmi's (the user's "wasmi-like")
  if speed asks.
- (b) wazero's interpreter converted by repr/go (Apache-2.0; a fetch; its structure is a compiler's IR built for its JIT).
- (c) The engine in Arbace forms, evaluated: too slow for Go-built modules (P2's 1.5 MB of code is millions of instructions to start).

Recommended: (a), (b) only if (a) cannot reach a useful speed.

### 6.2 The machine

- **Store and instances**: functions (Wasm's and the host's), tables (`funcref`, `externref`, typed references), memories (a byte slice
  per memory, page size 64 KiB, `memory.grow` by reallocation with the limit; memory64's 64-bit addresses), globals, tags, element and
  data segments with their dropped state; the linker by import names, `register`'s namespace.
- **Traps** as the spec names them (`unreachable`, `integer divide by zero`, `integer overflow`, `out of bounds memory access`,
  `indirect call type mismatch`, `undefined element`, `uninitialized element`, `null reference`, `cast failure`, ...), a Go error value,
  never a Go panic across the engine's boundary.
- **Exhaustion**: a call-depth counter (`assert_exhaustion`: "call stack exhausted"), never Go's stack overflow (fatal, uncatchable).
- **Floating point**: Go's IEEE `float32`/`float64`; every result forced through an explicit conversion so that gc never fuses a
  multiply and an add (the Go spec permits fusion; gc does it on arm64: a Wasm `f32.mul` then `f32.add` must round twice); `min`,
  `max`, `nearest`, the conversions' edges and NaN propagation by the spec's rules (any arithmetic NaN; canonical where required).
- **SIMD128**: `v128` as two `uint64`, each lane operation written out (no host SIMD); relaxed SIMD's operations take one of the
  spec's allowed results, fixed and documented (the deterministic profile's choice where it names one).
- **GC** (WA8): structs and arrays as Go values the Go GC collects, `i31ref` unboxed, subtyping by the spec's rules over the type
  section's recursive groups (iso-recursive canonical types).
- **Threads** (WA13): shared memories and atomics over `sync/atomic`, threads as goroutines, `memory.atomic.wait`/`notify` on
  per-address queues; WAST's `thread` and `wait`.

### 6.3 Breadth, in order (WAS4)

1. **Wasm 2.0**: the MVP's instructions, sign extension, saturating conversions, multi-value, bulk memory and tables, reference types,
   multiple tables. Everything Go emits (P3) and LLVM 23 enables by default (to verify: LLVM 20 made bulk memory, nontrapping conversions,
   multivalue and reference types its defaults).
2. **SIMD128**: 59 `simd_*` scripts (P8); Go's SIMD experiment and clang's `-msimd128` produce it.
3. **Wasm 3.0 without GC**: tail calls (`return_call*`), exceptions (`try_table`, `throw`, `throw_ref`, `exnref`; `legacy/`'s
   `try`/`catch`/`delegate` beside), memory64, multiple memories, extended constant expressions, typed function references
   (`call_ref`, `ref.as_non_null`, `br_on_null`).
4. **GC**: `struct.*`, `array.*`, `i31`, `ref.test`, `ref.cast`, `br_on_cast`, `any.convert_extern`; the type system's subtyping.
5. **Relaxed SIMD**, then **threads**, then the testsuite's `proposals/` beyond 3.0 (custom page sizes, wide arithmetic, ...): each
   classed `:deferred` until built.

### 6.4 The hosts

- **`spectest`** (the testsuite's host module): `print`, `print_i32`, ... (writing to the run's output), `global_i32` (666), `global_i64`,
  `global_f32`, `global_f64`, `table` (10 to 20 funcrefs), `memory` (1 to 2 pages). Its spec is the reference interpreter's
  `spectest.ml`, a few dozen lines.
- **WASI preview 1** (`wasi_snapshot_preview1`): the 38 functions Go's `wasip1` port imports (P3) over a sandboxed directory tree (the
  preopened directories), the clock, randomness (seeded for checks), arguments and environment; `poll_oneoff` for Go's timers and its
  netpoll; no sockets but `sock_accept` refusing. crt's `Sys` (C's run time) is the model of a kernel made of data; this one is smaller.
- **The headless `gojs` world**: the 24 imports of `wasm_exec.js` (P3) implemented in Go over a small object model of JavaScript values
  (the reference table `syscall/js` keeps on the JS side: numbers, strings, `undefined`, `null`, booleans, objects as maps, functions,
  `Uint8Array` over a byte slice, promises resolved by the world's own event loop), with a fake `globalThis` holding `fs` (the
  asynchronous file system the shim also has, §9.2), `crypto.getRandomValues`, `performance.now`, `setTimeout`, and the **recording
  fakes** of the browser: a `document` with one canvas, `requestAnimationFrame` stepped by the check, input events injected from a
  script, `navigator.gpu` returning objects that record every call and the command buffers. It is what lets Go's `js/wasm` programs,
  Arbace's REPL and the demo run and be judged with no browser (§10.3).

### 6.5 What the other crops gain

- **Go**: goi's check gains a third target (`wasip1`) for `$GOROOT/test`, its wasm-specific tests (`test/wasmexport*.go`,
  `wasmmemsize.go`) runnable; Go's `syscall/js` tests runnable on the headless world.
- **C, C++, Rust, LLVM IR**: their wasm32 output (clang `--target=wasm32-wasip1`, rustc's `wasm32-wasip1`: packs at the oracle tier) is a
  new run corpus for each crop's programs, judged by the same own verdicts as their native runs.
- **OCaml**: the spec's reference interpreter is an OCaml program; running it under oi is a large real program for oi's check (WAS19).

## 7. AOT: w2g

### 7.1 What AOT means here (WAS10)

- (a) **Wasm → Go forms** (`arbace.wasm.w2g`): each function a Go function over the instance's state (locals as Go variables, the
  operand stack resolved to temporaries at translation, structured control as Go's labeled blocks and loops, `br_table` a `switch`,
  traps as returns of an error, memory accesses bounds-checked through `encoding/binary`-like helpers), the host imports as Go
  interfaces satisfied by wai's hosts, printed by repr/go and built by TamaGo's go. The other crops' meaning of AOT; runs on amd64,
  arm64 and in the box.
- (b) **Native code** through the asm crops' encoders and ELF writers (a Cranelift-like compiler): the bytes are Arbace's; much later.
- (c) **Wasm → C forms** as `wasm2c` does, then ci and cf2g: possible, longer, and C's route adds nothing over (a).

Recommended: (a); (b) optional, after the asm crops' AOT cells are whole.

### 7.2 The unit and the cost

One Go program per WAST script (its modules translated, its commands as the program's `main`, the results printed in the record's
format) and one per run-corpus module; built by `arbace.build.go` with a print cache. 258 root scripts at a few seconds each: a night's
rotation (GT8.4's `:rotate`), an essential handful cached by key.

## 8. Arbace to Wasm

### 8.1 What Go's port gives

TamaGo's go builds `GOOS=js` and `GOOS=wasip1` for `GOARCH=wasm` (P1): one thread, goroutines scheduled cooperatively (the runtime's
own stack switching in the wasm back end; no shared-memory threads), `GOMAXPROCS` 1, the GC on that thread, linear memory up to 4 GiB
(wasm32), a module of about the native size (P1). On `js` the runtime blocks by returning to the JavaScript event loop and is resumed by
callbacks (`scheduleTimeoutEvent`, `js.FuncOf`); on `wasip1` it blocks in `poll_oneoff`. `syscall/js` reaches the host's objects
through a reference table on the JS side; each call crosses the boundary (microseconds: §9.3's command buffer is about this).

### 8.2 What the program needs (WAS12)

- **jrt's host layer for js and wasip1**, a third variant beside `*_linux.go` and `*_tamago.go` (P4): files (`hostfs`: on wasip1 WASI's
  preopens; on js Go's `os` over the shim's asynchronous `fs`, a memory file system with read-only HTTP mounts: an open of a mounted path
  is a `fetch`, the goroutine parked until it resolves), processes (`hostproc`, `spawn`: none; `ProcessBuilder` refuses with the box's
  message), randomness (`crypto.getRandomValues`, WASI's `random_get`), the network (`net`: none on wasip1; on js `fetch` only, as an
  `HttpClient` transport), resources and the store (in memory, or the browser's IndexedDB behind the same `fs`).
- **goi's table's parts** for `js/wasm` and `wasip1/wasm` (`gotable/*_sN.go`: the generator `arbace.go.table` given the two targets).
- **The runtime overlay** (`overlay/go/runtime/proc.go`, `runtime2.go`, `arbace_local.go`): a printed copy of go1.27.1's with Arbace's
  goroutine-local slot and event hook; it keeps the wasm paths of the scheduler (`beforeIdle`), to verify by a build.
- **The REPL's input** (WAS13): on js, jrt's standard input is a channel fed by a `js.FuncOf` callback the page calls with each line; the
  reader's goroutine parks and Go returns to the event loop (the box's `arbace_eventPark` hook is the same idea). Output goes to the page's
  console element through `wasmWrite` / `fs.writeSync`.
- **`bin/arbace-go --build --wasm js|wasip1`** and **`--build-program --wasm`** (REAP7's user programs for the browser).

These files are the runtime's: they move `gate_key` and `arbace_runtime_key` (their `//go:build js` lines do not keep them out of the
keys' hashes). They wait for a key window (W, or the next re-seed's), WAS24; everything before WA9 moves no key.

### 8.3 Size and start (to measure, WA9)

Estimates from P1 and P5: about 215-220 MB of module (60-65 MB gzip, about 45 MB brotli or xz), compiled by a browser's baseline
compiler in seconds (V8's Liftoff streams it), a REPL heap of about 160 MB, well inside wasm32's 4 GiB. If the size matters, a REPL
build without the tools (c2j, j2g, repr/*, the crops' namespaces, goi's table) through `--build-program` (WAS14) is the first lever;
measuring comes first.

### 8.4 Arbace inside Arbace's Wasm

The `wasip1` build run by wai (and by w2g's translation of it) is the crop's strongest self-test: Arbace's whole runtime as a Wasm
module, its REPL evaluating `(+ 1 2)` under an engine Arbace wrote, as the box runs Arbace under Arbace's monitor. Interpreted it will be
slow (estimate: tens of seconds to start; the night's), w2g's native build near the original's speed.

### 8.5 A direct back end, later (WAS25)

Forms → Wasm without Go: c2j's Java forms to Wasm GC (as J2CL's and Kotlin/Wasm's back ends do), a smaller module with the host's GC.
Large, and only worth it once wai has GC (WA8) and the browser path is used. A survey of its own then.

## 9. The browser side

### 9.1 How the single-page WebGPU games are built

The pattern of the current single-page games and demos (wgpu's and Bevy's web builds, Dawn's `emdawnwebgpu`, sokol, the WebGPU samples):

- One page, one `<canvas>` filling the window, sized by a `ResizeObserver` times `devicePixelRatio`; no framework, often no bundler.
- Start: `navigator.gpu.requestAdapter()`, `adapter.requestDevice()`, `canvas.getContext("webgpu")`, `context.configure({device, format:
  navigator.gpu.getPreferredCanvasFormat(), alphaMode: "opaque"})`; shader modules from WGSL text; pipelines, buffers, bind groups made
  once.
- A frame loop on `requestAnimationFrame(t)`: input applied, the simulation stepped (often a fixed time step with the remainder
  interpolated), uniforms written with `device.queue.writeBuffer(buffer, offset, view)`, then **one command encoder a frame**:
  `beginRenderPass` on `context.getCurrentTexture().createView()` (clear, store), `setPipeline`, `setBindGroup`, `setVertexBuffer`,
  `draw`/`drawIndexed`, `end`, `queue.submit([encoder.finish()])`; compute passes the same way.
- Input from `keydown`/`keyup` into a key map, pointer events (with pointer lock for camera control), `wheel`, gamepads polled each frame.
- Assets by `fetch` (`arrayBuffer()` for meshes and data, `createImageBitmap(blob)` and `queue.copyExternalImageToTexture` for images).
- For Wasm: the game's state lives in linear memory; vertex and uniform data are written to the GPU straight from a typed-array view of
  that memory (`new Float32Array(memory.buffer, ptr, n)`), recreated when memory grows (the buffer is detached); the JS glue holds a
  handle table (small integers ↔ GPU objects), because Wasm code cannot hold JS objects (without reference types).
- Served from any static server with `application/wasm` (for `instantiateStreaming`); WebGPU needs a secure context (https or
  `localhost`); no COOP/COEP unless threads are used.

### 9.2 The shim (WAS15)

Two files and the module, under `wasm/web/`, no npm, no build step:

- **`index.html`** (about 30 lines): the canvas, a `<pre>` console and an input line for the REPL, one `<script type="module"
  src="arbace.js">`.
- **`arbace.js`** (about 350-450 lines, estimate), in parts:
  1. the `gojs` imports, derived from Go 1.27.1's `wasm_exec.js` (575 lines, BSD-3; its origin and revision recorded beside it and in
     LICENSE.md's Go section): the 24 functions, the value table, `resetMemoryDataView` keeping the memory views current;
  2. `globalThis.fs`: Go's asynchronous file system protocol (`open`, `read`, `write`, `fstat`, ... with callbacks, as `syscall/fs_js.go`
     calls them) over a memory file system, standard input fed by the input line, output to the console, read-only mounts fetched over
     HTTP (`/assets/`), optionally persisted to IndexedDB;
  3. the frame loop: `requestAnimationFrame` calling the Go side's registered frame function with the time;
  4. input: keyboard, pointer, wheel, resize, delivered as events into a ring buffer in linear memory the Go side drains each frame (one
     callback a frame, not one an event);
  5. the WebGPU bridge (§9.3);
  6. start: `WebAssembly.instantiateStreaming(fetch("arbace.wasm"), imports)`, `go.run(instance)`, errors to the console.
- A check (own verdict, no browser): the shim's `gojs` import names equal the pinned Go's `//go:wasmimport gojs` list and
  `wasm_exec.js`'s (24 today), so a Go update cannot leave the shim behind silently.
- A small server for development (`bin/wasm serve DIR`, a Go program built by TamaGo's go, `net/http.FileServer` with the Wasm MIME
  type on `localhost`), not a dependency of any check.

### 9.3 The WebGPU bridge (WAS16)

- **Setup** through `syscall/js` (rare calls: adapter, device, context, shader modules, pipelines, buffers, textures, bind groups),
  each promise awaited by a channel; the objects kept in the shim's handle table, Go holding handles.
- **The frame** as one **command buffer** Go writes into linear memory (operations named after `webgpu.h`: `BeginRenderPass`,
  `SetPipeline`, `SetBindGroup`, `SetVertexBuffer`, `Draw`, `DrawIndexed`, `DispatchWorkgroups`, `WriteBuffer(handle, offset, ptr, len)`,
  `End`, `Submit`), decoded by the shim in one call; `WriteBuffer` passes a view of linear memory, no copy through Go.
- The same buffer is what the headless world records (§6.4): the per-frame stream is data, and so judgeable.
- Breadth later (WAS16 (c)): the Go API generated from `webgpu.idl` as data, the bridge's operation table with it.
- Canvas 2D: the console and text only; a WebGPU-less browser gets a message, not a fallback renderer.

### 9.4 The demo

A small scene, in two steps (WAS22):

1. **Go forms** (`test/programs/wasm-scene/`, a REAP7 program with a `go/` package): a lit, textured rotating cube and a compute-shader
   particle field; keyboard and pointer to move the camera; a texture and a mesh fetched as assets; a fixed 60 Hz step. Built by
   `--build-program --wasm js`.
2. **From Arbace**: the same scene driven by a Clojure namespace (`arbace.web.gpu`, a thin API over the bridge), with the REPL in the
   page: a form typed at the REPL changes the next frame (colour, speed, a shader recompiled): the self-sustaining REPL's browser analog.

## 10. The judges

Every judge is an own verdict but the oracle tier's (§11).

### 10.1 The testsuite

- Each command a case: `module` (decodes, validates, instantiates), `assert_return` (the results equal, floats bit for bit, `nan:canonical`
  and `nan:arithmetic` by the spec's patterns, references by kind, `either` for relaxed SIMD), `assert_trap` (trapped; the message a
  second level), `assert_exhaustion`, `assert_exception`, `assert_invalid`, `assert_malformed`, `assert_unlinkable`,
  `assert_uninstantiable`.
- The read cell takes `module`, `assert_malformed`, `assert_invalid` and the round trips; eval and AOT take the rest.
- Messages: a rejection passes its level; the message is judged by prefix as the reference runner does, a `:beside` source (WAS8).
- Controls (GTS81 (a)): a script's assertions judged against another script's expected values must fail; an `assert_malformed` judged as
  a `module` must fail; a module's bytes with one byte flipped against its own forms must fail.

### 10.2 The run corpora

- `$GOROOT/test`'s programs built for `wasip1` by TamaGo's go and run by wai and by w2g's build: their `.out` files and headers (`// run`,
  the expected output) first, gc's linux run of the original where none (goi's judge, CSP2's ruling: gc is pinned, independence 1);
  the programs Go's own runner skips on wasm (`test/run.go`'s list) classed `:na`.
- Go's `js/wasm` programs (`syscall/js`'s tests, the demo, Arbace's REPL session `test/goi/session.in` on the js build) on the headless
  world, by their stated outputs.
- clang's and rustc's wasm32 output of the C, C++ and Rust run corpora: packs at the oracle tier (0.5), later.

### 10.3 The browser part: what is honest

- At check time: **the Go side** of a browser program runs on wai's headless world, and its observable behaviour (the command stream per
  frame, the uniforms written, the console, the files fetched) is compared with what the program states (the demo's `expected/frames.edn`:
  each frame's command digest and a few decoded values, written by its authors from the scene's arithmetic: the cube's matrix at frame
  N). That judges Arbace's half and the bridge's protocol. It does not judge JavaScript, WebGPU or a GPU.
- **The shim's JavaScript** is not run at check time (no JS engine: P7). Its import list is checked against Go's (§9.2); its code is
  small and reviewed. A JavaScript crop would be its own survey (WAS17 (b)).
- At the oracle tier, by hand at a tag: **headless Chromium** with WebGPU on a software adapter (SwiftShader), the demo loaded from
  `bin/wasm serve`, N frames stepped, each frame read back (`copyTextureToBuffer`, or the canvas's image) and recorded as a pack; compared
  with a tolerance (rasterization differs by driver), plus the console's text exactly. The Mac (Safari and Chrome with Metal) by hand: a
  checklist as the box's Mac bundle has.
- Not a REAP cell: the row's four cells are the Wasm core's; the browser is a demo with its own records.

## 11. Independence and the oracle tier

| stage | pipeline (check time) | judge (check time) | score (proposed) |
|---|---|---|---|
| read | repr/wasm, Arbace's alone; inputs hand-written (the testsuite: 1) or built by TamaGo's go (1) or clang packs (0.5) | own verdicts | about 0.95 to 1 |
| eval | wai and its hosts | the testsuite's assertions; Go's `.out` and gc (1) | 1 |
| AOT | w2g, repr/go's print, TamaGo's go | the same | 1 |
| print | repr/wasm | `:full` bytes, the forms | 1 |
| judge | `arbace.wasm.check`, reap's reader | | 1 |

**The oracle tier** (`:phase :oracle`, `bin/gate --oracle`): hosts `:wasm-tools` (wabt's `wat2wasm`, `wasm2wat`, `wasm-validate`,
`spectest-interp`; wasmtime; node) and `:browser` (chromium), toolchain hosts beside `:llvm` (crops-score's list gains them, a
`test/crops/independence.edn` note). Packs: `wasm-wabt-bin` (wabt's binaries of the testsuite's text modules: the encoder compared
byte for byte where wabt's choices are the spec's), `wasm-wasmtime-runs` (wasmtime's runs of the `wasip1` corpus), `wasm-clang-wasip1`
(clang's modules of C's run corpora), `wasm-browser-frames` (§10.3). Steps `wasm-oracle-live` and `wasm-records-verify` (ORS7 (a)'s
crossed pair). No check-time step runs wabt, wasmtime, node, clang or a browser.

## 12. The corpora

| corpus | where | size | levels | own verdict | licence (as data) |
|---|---|---:|---|---|---|
| **WebAssembly/testsuite** | fetch, one commit (WAS5) | 293 `.wast`, about 56,500 assertion lines (P8, to verify) | all four | the assertions | Apache-2.0 |
| **Go-built modules** | built at check time by TamaGo's go from `$GOROOT/test`'s programs and `test/wasm/go/`'s | 1,837 candidate programs, about 2.5 MB each (P1) | read, print (binary); eval, AOT | `.out`, headers, gc's run | BSD-3 (Go's) |
| **Go's js programs** | `syscall/js`'s tests, `test/programs/wasm-scene`, the REPL session | tens | eval (headless) | stated outputs, `expected/frames.edn` | BSD-3, Arbace's |
| **Arbace's own build** | `bin/arbace-go --build --wasm` (WA9) | one module, about 215 MB (P5) | read, print, eval | the smoke's outputs | Arbace's |
| **synthetic modules** | generated by a committed generator per instruction family (edge values, results printed through `spectest.print`) | a few hundred | all four | the generator's stated values from the spec's definitions | Arbace's |
| `MC/WebAssembly`, `lld/test/wasm` | the pinned llvm-project tarball (offline) | 106 and 310 files | read (WAS20, later) | CHECK lines | Apache-2.0 WITH LLVM-exception |
| clang's wasm32 packs | oracle tier | C's run corpora | eval, AOT | exit status | per corpus |

**To fetch** (each by pin, the user's go-ahead; sizes to verify):

- **WebAssembly/testsuite** at one commit (GitHub's tarball, a few MB compressed, about 293 `.wast`; WA2). Apache-2.0: a LICENSE.md
  section.
- **WebAssembly/spec** at its 3.0 tag (optional, WAS19: the OCaml reference interpreter and the document's validation appendix for
  reading; about 10-20 MB). Apache-2.0 (the interpreter, tests); W3C (the document, read only).
- **wazero** at a release (optional, reading only unless WAS6 (b); a few MB). Apache-2.0.
- **`webgpu.idl`** (gpuweb/gpuweb, about 50 KB; only with WAS16 (c)).
- **The oracle tier's packages** (Alpine, only when the user wants the tier): `wabt` (a few MB), `wasmtime` (to verify the repository:
  community or testing; tens of MB), `nodejs` (about 30 MB apk, 90 MB installed), `chromium` (about 110 MB apk, 400 MB installed, with
  SwiftShader's WebGPU to verify on Alpine's build).

## 13. Layout, row, gate and budget

### 13.1 Layout (WAS3)

| piece | proposed |
|---|---|
| reader, printer | `arbace.repr.wasm` (`arbace/repr/wasm{.ae,/}`): `binary` (decode, encode), `lex`, `text` (WAT), `wast`, `full`, `print` |
| validator, AOT, check | `arbace.wasm.valid`, `arbace.wasm.w2g`, `arbace.wasm.check` (`arbace/wasm/`) |
| engine | `go/arbace/wai` (Go forms, hand-written, a light driver): `decode`, `valid` (its own, judged against Arbace's: a `:beside` source, as x86i's decoder), `exec`, `simd`, `gc`; hosts `spectest`, `wasi`, `gojs` |
| shim, demo | `wasm/web/index.html`, `wasm/web/arbace.js`; `test/programs/wasm-scene/` |
| records | `test/wasm/` (`corpora.edn`, `essential-*.txt`, `{read,eval,aot,print}-results.edn`, `divergences{,-read,-aot,-print}.edn`, `controls.edn`, `records/`) |
| commands | `bin/repr-wasm read|print|roundtrip|check`, `bin/wai run|check|check --aot|divergences`, `bin/wasm serve|build`; Makefile `wasm-*` |
| spec | `doc/wasm/SPEC.md` (the forms), `doc/wasm/WAI.md` (the engine, its hosts) |

### 13.2 Keys

All of WA1-WA8's and WA10's paths in `arbace_crop_paths` (`bin/lib/version.bash`): `arbace/repr/wasm.ae`, `arbace/repr/wasm/*`,
`arbace/wasm/*`, `go/arbace/wai.ae`, `go/arbace/wai/*`, `bin/lib/wasm/*`, and `wasm/` and `test/wasm/` outside every key already (no key
hashes them). `test/gate/gate-key-crops-test.bash` proves no key moves. WA9's jrt and table files are the runtime's (§8.2): a key
window (WAS24).

### 13.3 The row

`test/reap/cells.edn` gains `:wasm` ("WebAssembly"), an intermediate row printed after the asm rows, four cells as §1, the readers
`:asm-rows`-like (rows `[ID RESULT REASON]`); `test/crops/crops.edn` its crop (`:steps #"^(wasm|wai)-"`, controls by family);
`test/crops/independence.edn` its five stages.

### 13.4 Gate steps (a GT9 fragment, `test/gate/crops/wasm.edn`)

| step | phase | command | memory, time (estimates) |
|---|---|---|---|
| `wasm-read-essential` | essential | `bin/repr-wasm roundtrip --only test/wasm/essential-read.txt` (about 30 scripts and 10 Go-built modules) | 1.5 GiB, 30-60 s |
| `wai-essential` | essential | `bin/wai check --only test/wasm/essential-eval.txt` with the controls | 1 GiB, 30-60 s |
| `wasm-aot-essential` | essential (from WA7) | `bin/wai check --aot --only test/wasm/essential-aot.txt` (two scripts, cached by key) | 2 GiB, 1-2 min warm |
| `wasm-roundtrip`, `wasm-valid` | full | the whole testsuite and the Go-built set, `:parts` by corpus | 2 GiB, 5-15 min |
| `wai-check` | full | the testsuite by proposal, `:parts` | 1.5 GiB, 3-10 min |
| `wai-go` | full | the `wasip1` programs, `:rotate` a seventh nightly | 2 GiB, 15-30 min |
| `wasm-aot` | full | w2g's programs, `:rotate` | 3 GiB, 20-40 min |
| `wasm-headless` | full | the js programs and the demo's frames on the headless world | 1 GiB, 1-3 min |
| `wasm-oracle-live`, `wasm-browser` | oracle | §11 | by hand |

### 13.5 The budget (WAS23)

Half a crop's share (as LLVM IR's and the asm crops'): essential about 2 minutes, nightly about 35 minutes with the rotations, clean
the whole (about 2 hours, estimate). The Go builds of the `wasip1` corpus are the cost (P1: about 10 s cold each, less warm), cached by
the program's and the toolchain's key.

### 13.6 Licences

LICENSE.md gains "WebAssembly testsuite: Apache License 2.0" (the test data in `.tmp/`, any committed essential copies), and, if
fetched, the spec's reference interpreter (Apache-2.0); the shim's origin in Go's section (BSD-3, already there); wazero's
(Apache-2.0) only if code is converted (WAS6 (b)); W3C's licence only if `webgpu.idl` is committed as data. wabt, wasmtime, node and
Chromium are only run.

## 14. Plan (WA1-WA12)

Working days, as the other surveys give them; each milestone ends with its check green and its records.

| milestone | content | exit criteria (measurable) | days |
|---|---|---|---:|
| **WA1** the binary format | `doc/wasm/SPEC.md` (forms); `arbace.repr.wasm.binary`: every section, every instruction of Wasm 2.0 decoded and encoded; `:full` (LEB widths, layout); `bin/repr-wasm read|print|roundtrip`; Go-built modules as the corpus (no fetch) | **20 Go-built modules (hello, goi's essential programs for `wasip1` and `js`) read, printed `:full` byte for byte, read again equal; every opcode Go emits decoded**; the canonical print re-read equal | 3-5 |
| **WA2** WAT and WAST | the lexer, WAT with its abbreviations, WAST's commands; canonical and `:full` text print; the cross-format judge; the testsuite fetched and pinned | **the testsuite's root scripts read: every module at the read and print levels, every `assert_malformed` (binary and quote) rejected**, the rest classed | 4-7 |
| **WA3** validation | `arbace.wasm.valid`, the spec's algorithm for 2.0 | **every 2.0 `assert_invalid` rejected, no valid module rejected**; messages at the `:beside` level | 3-5 |
| **WA4** wai, 2.0 | `go/arbace/wai`: decode, validate (its own), the internal code, the interpreter; `spectest`; the WAST driver; traps, exhaustion | **the testsuite's 2.0 scripts at eval: none failing, each non-pass classed**; the controls fail as they must | 6-10 |
| **WA5** WASI and Go | WASI preview 1; `$GOROOT/test` built for `wasip1` and run, judged by `.out` and gc | **goi's essential list on wai, then the whole corpus classed**; `wasmexport*.go` and `wasmmemsize.go` | 5-8 |
| **WA6** SIMD128 | the 59 scripts; relaxed SIMD's fixed choices | the `simd_*` scripts at eval | 4-7 |
| **WA7** AOT | w2g; one program a script; the `wasip1` corpus through w2g | **the 2.0 and SIMD scripts at AOT equal to eval**; the Go programs at AOT | 7-12 |
| **WA8** 3.0's rest | tail calls, exceptions (and `legacy/`), memory64, multiple memories, extended constants, typed function references; then GC | the root scripts of each at read, eval and AOT | 10-18 |
| **WA9** Arbace to Wasm | jrt's js and wasip1 variants, goi's table's, `--build --wasm`, `--build-program --wasm` (at a key window, WAS24) | **Arbace's `wasip1` module runs `-e '(+ 1 2)'` on wai**; sizes and start measured; the oracle tier's wasmtime and node smokes | 6-10 |
| **WA10** the row | `cells.edn`, classes, controls, the fragment, `crops.edn`, `independence.edn`, Makefile, the budget measured (`bin/gate --costs`); bring forward to after WA4 if the user wants the row early, as AX10 was | `bin/reap`: four cells checked, none failing; `bin/crops-score --crop wasm` printed | 2-3 |
| **WA11** the headless world | `gojs` in wai: the JS value model, `fs`, timers, the recording fakes (canvas, rAF, input, `navigator.gpu`) | `syscall/js`'s tests and Arbace's js build's REPL session on it, by their outputs | 4-7 |
| **WA12** the browser | the shim, `bin/wasm serve`, the WebGPU bridge, the demo (Go, then Arbace with the REPL in the page); `expected/frames.edn`; the oracle tier's headless Chromium pack; the Mac checklist | the demo's frames equal on the headless world; by hand: the scene in Chrome and Safari | 6-10 |
| *WA13* (optional) threads | shared memory, atomics, `wait`/`notify`, WAST's `thread` | `proposals/threads` | 4-6 |
| *WA14* (optional) the spec's interpreter on oi | WebAssembly/spec's `interpreter/` read by repr/ocaml, run by oi as a second judge | the testsuite's 2.0 scripts on it, equal to wai's verdicts | 3-6 |

**Total without WA13 and WA14: about 62 to 105 working days.** The least known are WA8's GC (the type system) and WA9 (the program's
build, its size and speed on Wasm, the key window). Re-estimate at WA4's end.

**Sonnet and Opus** (the user's split): Sonnet for WA1's sections, WA2's grammar and `:full`, WA5's WASI calls, WA10, WA12's shim and
demo. Opus for the validator (WA3, WA8's GC typing), wai's internal code and NaN rules (WA4, WA6), w2g's control flow (WA7), WA9's runtime
port and WA11's JS value model.

**Re-seed needs**: none until WA9 (§8.2).

## 15. Proposals (WAS1-WAS26)

Each with options, the recommended one named.

- **WAS1 The row.** (a) one intermediate crop `:wasm`, its own REAP row after the asm rows; (b) a part of Go's row (Go's third target);
  (c) two crops, the core and the web. *Recommended: (a)*: the testsuite is the subject's own and every producer's; the browser is a demo
  (WAS17).
- **WAS2 The forms.** (a) the spec's abstract syntax, one vocabulary for binary and text, names and spellings in metadata, `:full` the
  rest (§4.2); (b) WAT's concrete syntax as forms (folded as written, the binary read into an unfolded text). *Recommended: (a)*: the
  binary has no text shape, and (b) makes two crops' worth of forms.
- **WAS3 The layout.** (a) §13.1 (`arbace.repr.wasm`, `arbace.wasm.*`, `go/arbace/wai`, `wasm/web/`, `test/wasm/`, `doc/wasm/`); (b) all
  under `arbace/wasm/`. *Recommended: (a)*, the crops' pattern.
- **WAS4 Proposals in scope, in order.** (a) 2.0 (what Go and clang emit), SIMD128, 3.0 without GC, GC, then relaxed SIMD and threads,
  `proposals/` beyond 3.0 deferred (§6.3); (b) the MVP alone, then by the testsuite's files; (c) all of 3.0 at once. *Recommended: (a)*.
- **WAS5 The testsuite's pin.** (a) WebAssembly/testsuite at one commit, each file tagged by proposal in `test/wasm/corpora.edn` (the
  cells counting what is built, the rest `:deferred`); (b) the spec repo's `test/core` at its 2.0 tag first, the 3.0 pin later.
  *Recommended: (a)*: one pin, the classes carry the order.
- **WAS6 The engine's route.** (a) Arbace's own interpreter in Go forms, a light driver, an internal code at load, wasmi's register IR
  later (§6.1); (b) wazero's interpreter converted by repr/go; (c) an engine in Arbace forms. *Recommended: (a)*, wazero and wasmi read as
  references.
- **WAS7 The engine's name.** (a) `wai`; (b) `wasmi` (the crops' pattern, but the name of a known engine: every document would need to
  say which); (c) `wasmi` under `bin/wasm run`. *Recommended: (a)*.
- **WAS8 The testsuite's judges.** (a) the assertions as stated, NaN patterns by the spec, a rejection or trap passing its level and the
  message a `:beside` level by prefix; (b) messages required equal. *Recommended: (a)*: the spec leaves the wording to engines.
- **WAS9 Validation's cell.** (a) read's (acceptance, as the asm crops' matching); (b) eval's. *Recommended: (a)*.
- **WAS10 AOT's meaning.** (a) Wasm → Go forms (w2g), built by TamaGo's go; (b) native code through the asm crops; (c) Wasm → C forms.
  *Recommended: (a)*; (b) optional later.
- **WAS11 The run corpora.** (a) `$GOROOT/test` built for `wasip1` by TamaGo's go at check time (not a host toolchain), judged by `.out`
  and gc; Go's js programs on the headless world; clang's and rustc's wasm32 packs later at the oracle tier; (b) the testsuite alone.
  *Recommended: (a)*.
- **WAS12 Arbace to Wasm.** (a) both GOOS, `wasip1` first (wai checks it) then `js` (the browser), jrt's third host variant as the box's
  was (§8.2); (b) `wasip1` only; (c) `js` only. *Recommended: (a)*.
- **WAS13 The browser REPL's input.** (a) a channel fed by a `js.FuncOf` callback, the reader's goroutine parked, Go back in the event
  loop; (b) a Web Worker with `SharedArrayBuffer` and `Atomics.wait` (needs COOP/COEP headers). *Recommended: (a)*.
- **WAS14 The module's size.** (a) build the shipped executable as is and measure, then a REPL program without the tools through
  `--build-program --wasm` if needed; (b) a dedicated slim build first. *Recommended: (a)*.
- **WAS15 The shim.** (a) Arbace's own `index.html` and `arbace.js`, the `gojs` part derived from the pinned `wasm_exec.js` (its origin
  recorded), its import list checked against Go's; (b) `wasm_exec.js` unchanged beside a separate bridge file; (c) a custom ABI (as
  TinyGo's) without `gojs`. *Recommended: (a)*: one small file, Go's ABI kept, the check catches drift.
- **WAS16 The WebGPU bridge.** (a) setup through `syscall/js`, the frame as one command buffer in linear memory named after
  `webgpu.h`, `writeBuffer` from memory views (§9.3); (b) everything through `syscall/js`; (c) (a) plus a Go API generated from
  `webgpu.idl`. *Recommended: (a)*, (c) when the demo outgrows it.
- **WAS17 The browser's judge.** (a) not a REAP cell; the Go side judged on the headless world by the program's stated frames; the shim's
  import list checked; a real browser only at the oracle tier (headless Chromium, software WebGPU, frames recorded with a tolerance) and
  on the Mac by hand; (b) a JavaScript crop first, so the shim runs at check time; (c) no judge of the browser part. *Recommended: (a)*.
- **WAS18 The oracle tier's hosts.** (a) `:wasm-tools` (wabt, wasmtime, node) and `:browser` (chromium), installed with the user's OK,
  only in `bin/gate --oracle`; (b) none, own verdicts only. *Recommended: (a)*, after WA4.
- **WAS19 The spec's reference interpreter.** (a) fetched, read by repr/ocaml and run by oi as a second judge (WA14), not a host
  toolchain; (b) at the oracle tier on the host's OCaml; (c) not used. *Recommended: (a)*, optional.
- **WAS20 LLVM's wasm assembly.** (a) a later syntax of repr/asm, its objects (`linking`, `reloc.*`) read by repr/wasm; (b) out of the
  crop. *Recommended: (a)*, after WA10.
- **WAS21 The fetches.** (a) the testsuite before WA2; the spec repo and wazero only for WAS19 and reading; the oracle tier's packages
  only with that tier; (b) none: Go-built modules alone. *Recommended: (a)*.
- **WAS22 The demo.** (a) Go forms first, then the same scene from a Clojure namespace with the REPL in the page; (b) Clojure first.
  *Recommended: (a)*: the Go side settles the bridge before Arbace's API on it.
- **WAS23 The budget.** (a) half a crop's share, as LLVM IR's and the asm crops'; (b) a full share. *Recommended: (a)*.
- **WAS24 The key window.** (a) WA1-WA8, WA10-WA12's shim in `arbace_crop_paths` (no key moves); WA9's jrt and table files at W or the
  next re-seed window; (b) WA9's files in a crop path copied into the build only for `--wasm`. *Recommended: (a)*: (b) hides runtime
  code from the keys.
- **WAS25 A direct back end.** (a) later, its own survey after WA8 (c2j's Java forms to Wasm GC); (b) now. *Recommended: (a)*.
- **WAS26 Threads.** (a) last (WA13): Go's wasm has none, so Arbace's own build does not need them; (b) with 3.0. *Recommended: (a)*.

## 16. What to decide first

1. **WAS1, WAS2**: the row and the forms (the spec waits on them).
2. **WAS6, WAS7**: the engine's route and name.
3. **WAS21**: the testsuite's fetch (WA2 needs it; WA1 does not).
4. **WAS12, WAS24**: Arbace to Wasm and its key window.
5. **WAS17**: what judges the browser part.

## 17. Risks

- **Speed of Go-built modules under an interpreter.** Go's runtime start is millions of instructions (P2's 1.5 MB of code), and
  `$GOROOT/test`'s programs are 1,837. Mitigation: the internal code at load, the rotation, w2g (WA7) for the heavy ones.
- **Floating-point fusion.** gc may fuse a multiply and an add (arm64): an unguarded `f32.mul` then `f32.add` would round once. Every
  result is forced through an explicit conversion, and the testsuite's float scripts run on arm64 too (the Mac VM).
- **NaN and relaxed SIMD nondeterminism.** The spec allows several results; the judges take the patterns, wai fixes and documents one
  choice, w2g must make the same.
- **The testsuite moves** (3.0 and proposals land often): one pin, re-pinned by decision.
- **Arbace's build on Wasm** (WA9): the runtime overlay's copy of `proc.go` on the wasm scheduler, j2g's runtime on one thread (Java
  threads as goroutines: a blocking call that holds the thread stalls the page), the GC's pauses on one thread, 4 GiB; the module's size in
  a browser. Mitigation: `wasip1` first under wai, measure, then js.
- **The browser is unjudged at check time.** Stated plainly (§10.3): the headless world judges the Go side; the shim is small and its
  ABI checked; frames only at the oracle tier.
- **Licences**: the testsuite Apache-2.0 as test data; Go's `wasm_exec.js` BSD-3 (already in LICENSE.md); the spec document W3C's,
  read only; GPL code none in sight.
- **The box**: wai is Go forms, so it runs in the box once the box runs again (parked by the user, 2026-10-26).

## 18. Sources

- The host: TamaGo's go1.27.1 (`/root/tamago-go`: `VERSION` go1.27.1, `lib/wasm/wasm_exec.js`, `wasm_exec_node.js`,
  `go_js_wasm_exec`, `go_wasip1_wasm_exec`, `misc/wasm/wasm_exec.html`, `src/runtime/{rt0_js_wasm,rt0_wasip1_wasm,asm_wasm,memmove_wasm,
  memclr_wasm,sys_wasm}.s`, `src/runtime/{lock_js,os_js,mem_js,note_js,os_wasip1,lock_wasip1,mem_wasip1,netpoll_wasip1,stubs_wasm}.go`,
  `src/syscall/js/js.go`, `src/syscall/fs_js.go`, `src/syscall/fs_wasip1.go`, `src/internal/buildcfg/cfg.go`'s `gowasm`,
  `src/cmd/compile/internal/wasm/{ssa,simdssa}.go`, `test/wasmexport.go`, `wasmexport2.go`, `wasmmemsize.go`); clang 23.1.5 and
  `wasm-ld`; `/usr/lib/llvm23/bin/llc --version`'s targets.
- The checkout at `ca3c7aff8`: `tamago/cmd/go/cmd/{compile/internal/wasm,internal/obj/wasm,link/internal/wasm}` (forms, 4,184 lines);
  `seed/arbace-seed-go.tar.xz` (the module's build constraints, `overlay/runtime/{proc,runtime2,arbace_local}.go`); `target/seed/amd64/
  arbace` (P5); doc/asm/AMD64-SURVEY.md, doc/asm/ARM64-SURVEY.md (the model), doc/CROPS-SCORE.md, doc/ORACLE-RECORDS-SURVEY.md,
  doc/go/GOI-SPEC.md, `test/reap/cells.edn`, `test/gate/crops/asm.edn`, `test/crops/crops.edn`, `bin/lib/version.bash`'s
  `arbace_crop_paths`.
- The pinned `llvm-project-23.1.5.src.tar.xz` (`.tmp/llvm/`): its listing for `llvm/test/MC/WebAssembly`, `CodeGen/WebAssembly`,
  `lld/test/wasm`, `llvm/lib/Target/WebAssembly`.
- Sourcegraph (anonymous, read-only, 2026-11-13): `github.com/WebAssembly/testsuite`'s `.wast` paths and `(assert_` count, its LICENSE.
- Prior art (not fetched; licences to verify): WebAssembly/spec `https://github.com/WebAssembly/spec`; WebAssembly/testsuite
  `https://github.com/WebAssembly/testsuite`; wazero `https://github.com/tetratelabs/wazero`; wasmi `https://github.com/wasmi-labs/wasmi`;
  wasm3 `https://github.com/wasm3/wasm3`; WAMR `https://github.com/bytecodealliance/wasm-micro-runtime`; wabt
  `https://github.com/WebAssembly/wabt`; wasm-tools `https://github.com/bytecodealliance/wasm-tools`; wasmtime
  `https://github.com/bytecodealliance/wasmtime`; WebGPU `https://github.com/gpuweb/gpuweb`; webgpu-headers
  `https://github.com/webgpu-native/webgpu-headers`; Dawn `https://dawn.googlesource.com/dawn`; wgpu `https://github.com/gfx-rs/wgpu`;
  Ebitengine `https://github.com/hajimehoshi/ebiten`; cogentcore/webgpu `https://github.com/cogentcore/webgpu`; sokol
  `https://github.com/floooh/sokol`.

## Appendix A: the scratch

The worktree's `.tmp/wasm/` (about 160 MB, kept): `hello/` and `hello-{js,wasip1,linux}*` (P1), `probe/` and `wdump` (P2's section
dumper), `add.c`, `add.wasm` (P6), `arbace-seed-go/` (the seed extracted, P4) and `list-{linux,js,wasip1}.txt` (its `go list`),
`llvm-wasm-list.txt` (P9), `sg*.txt`, `ts-files.txt`, `root.txt` (P8's Sourcegraph answers), `fib.ae` (P5). Each command ran in seconds
under 0.3 GiB; the commands are in §3.
