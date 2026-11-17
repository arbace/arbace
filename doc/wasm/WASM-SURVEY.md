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

## As built: WA1 (2026-11-13, branch `wasm-wa1`)

The user's decisions of 2026-11-13: WAS1-WAS26 all as recommended, and the fetches of WAS21 approved, each pinned. Built from
`mac/wasm-survey` (`837db6460`).

**The fetches** (`test/wasm/corpora.edn`'s `:pins`, `bin/repr-wasm corpus fetch|check`; into the main checkout's `.tmp/wasm/src/`,
extracted under `.tmp/wasm/tree/`; SHA-256 and SHA-512 each):

| pin | revision | file | bytes | extracted |
|---|---|---|---:|---:|
| WebAssembly/testsuite | `92a3d806c7d70f5b63f125e6155b8d76304509f1` | `testsuite-92a3d806.tar.gz` | 605,183 | 12.3 MB |
| WebAssembly/spec | tag `wg-3.0`, `9d36019973201a19f9c9ebb0f10828b2fe2374aa` | `spec-wg-3.0.tar.gz` | 12,763,603 | 46.5 MB |
| tetratelabs/wazero | tag `v1.12.0`, `2ab480b55fa408d6b35df97fe32a60d08bd6e201` | `wazero-v1.12.0.tar.gz` | 4,279,128 | 42.7 MB |
| gpuweb/gpuweb `webgpu.idl` | `gh-pages` `025b9eaa4e3c3fc956d5f31a72540a15f33dfdf7` | `webgpu-025b9eaa.idl` | 38,683 | |

P8 verified on the pin: 293 `.wast` (258 at the root, 28 under `proposals/`, 4 `legacy/`, 3 `custom/`), 11.8 MB, 64,236 `(assert_`
occurrences (64,231 lines that begin with one; the survey's Sourcegraph estimate was about 56,500). `webgpu.idl` is not on gpuweb's
main branch (the IDL is generated from `spec/index.bs`): the pin is the published copy on `gh-pages`. The oracle tier's packages
(WAS18 (a)): `apk add wabt wasmtime nodejs` (Alpine edge; all three present: wabt 1.0.41-r0 from testing, wasmtime 47.0.3-r1 from
community, nodejs 24.22.3-r0 from main), recorded in `:oracle-packages`; no check-time step runs them. Chromium not installed.

**What was built** (doc/wasm/SPEC.md §1-§6):

- `arbace.repr.wasm.binary`: the binary format decoded into Wasm forms and encoded back, Wasm 3.0's whole binary grammar (types with
  GC's `rec`, `sub`, `struct` and `array`; imports, functions, tables with initializers, memories with i64 limits, tags, globals,
  exports, start, the eight element segment encodings, data count, code, data, custom sections), decode.ml's order of reads, checks
  and messages; the name section read into the functions' `:name` and `:local-names` when it prints back byte for byte.
- `arbace.repr.wasm.ops`: the instruction table, **493 instructions** (every one of Wasm 3.0 but the four structured ones: SIMD128,
  relaxed SIMD, bulk memory and tables, reference types, GC, tail calls, exceptions), generated from the spec's pin by
  `test/wasm/gen_ops.ae` (`bin/repr-wasm ops gen|check`: decode.ml's `instr` arms classed by the shape of their immediates' reads into
  20 kinds, lexer.mll's keywords their names; an arm of an unknown shape fails the generation).
- **Every opcode Go emits** (`test/wasm/go_ops.ae`, in `ops check`): the 446 instructions of Go's wasm assembler
  (`$GOROOT/src/cmd/internal/obj/wasm`'s `a.out.go`, numbered by `writeOpcode`'s ranges) are all in the table, under Go's name (five by
  Go's own spellings: `CurrentMemory`, `GrowMemory`, `I8x16Shuffle16`, two relaxed truncations without `_zero`). A finding, to report
  upstream or to verify with a `GOEXPERIMENT=simd` build: `writeOpcode` writes, for the 128 SIMD instructions whose u32 is 0x80 or more,
  a byte (0x01 or 0x02) after the u32's LEB128, which the spec's encoding does not have (and for the relaxed ones the u32 itself is
  0x80 less than theirs); Go's default build emits no SIMD, so no module of the corpus has it.
- Mode `:full` (§5): the LEB128s' widths by ordinal, the empty sections, the segments' flags and the locals' grouping when not
  canonical; the name section's own widths. Canonical printing writes minimal LEB128s.
- `arbace.repr.wasm`: the `.ae` text (an item a line, a function's instructions a line each), its reading back, the round trip
  `check-one` and the negative `control`; `bin/repr-wasm` (read, print, corpus fetch|check|build|lists, roundtrip, divergences, ops).
- The crop's paths in `bin/lib/version.bash`'s `arbace_crop_paths` (`arbace/repr/wasm.ae`, `arbace/repr/wasm/*`, `arbace/wasm/*`,
  `go/arbace/wai.ae`, `go/arbace/wai/*`, `bin/lib/repr-wasm/*`, `bin/lib/wai/*`), mirrored in `arbace/j2g/embed.ae`'s excluded and the
  audit's `crop-rules` (`test/gate/audit.ae`); `test/gate/gate-key-crops-test.bash` passes (347 checks): a file under any of them moves
  no key. The lists' own bytes move `gate_key` once (`6a2138a2...` → `dbf88e97...`, as AX1's did, `ab91839f8`): the branch merges in
  a key window (W2's end or the next), WAS24.

**The corpus** (WAS11 (a)): `bin/repr-wasm corpus build` builds ten Go programs (`test/wasm/go/hello/main.go`, Arbace's: a goroutine,
a channel, fmt, time; and nine `// run` programs of `$GOROOT/test` from goi's essential list: `235.go`, `chan/sieve1.go`, `defer.go`,
`ken/divconst.go`, `ken/cplx2.go`, `float_lit.go`, `map.go`, `recover.go`, `zerodivide.go`) by TamaGo's go1.27.1 for `wasip1` and `js`
(20 modules, 23.5 and 23.7 MB, 1.9 to 3.8 MB each; about 23 s cold), keyed by the toolchain's VERSION and the sources' digest.

**The round trip** (`bin/repr-wasm roundtrip`, the module's own verdict): **20 of 20 modules pass** at every level (decoded in :full,
the `.ae` text, 22 to 41 MB each, read back to the same items and layer, printed back byte for byte, the canonical encoding decoded
again to the same items with no layer), recorded in `test/wasm/roundtrip-results.edn` (no class needed: `test/wasm/divergences-read.edn`
is empty of round-trip classes). 22,064 to 52,267 items a module (a function an item); the layer holds 133 to 1,231 non-minimal
LEB128s a module (Go's linker pads the section and body sizes and its relocated operands to 5 bytes; the wasip1 ones and the js ones
alike); the js modules' name section reads into the functions' names (1,647 in hello's). The modules use 107 distinct instructions
(`.tmp/repr-wasm/rt/ops.tsv`): the MVP's integer, float, memory and control instructions, sign extension (`i64.extend8_s` ...),
saturating truncation (`i64.trunc_sat_f64_s`, `_u`) and bulk memory (`memory.copy`, `memory.fill`), as P3 said. The negative controls
(`test/wasm/controls.txt`): a module's :full print against another program's bytes, against the other GOOS's, against its own bytes
with the middle byte flipped: each differs, as it must; the positive check (a module against its own bytes) is the same. Measured
(stage 0, by hand, one run): 1,322 s for the 20 modules and the four controls, about 65 s a module, peak 1.55 GB (GOMEMLIMIT
1500MiB, one module at a time). `bin/repr-wasm ops check` (the table regenerated byte for byte and Go's 446) a few seconds.

**Not done in WA1**: `:full` for the text format (WA2's), the custom sections as the text's `(@custom ...)` annotations, a gate
fragment (`test/gate/crops/wasm.edn`, WA10's: the essential steps of §13.4 measured first).

## As built: WA2 (begun, 2026-11-13, branch `wasm-wa1`)

**What was built** (doc/wasm/SPEC.md §7, §8):

- `arbace.repr.wasm.lex`: the text format's lexer (keywords, `$id` and 3.0's `$"..."`, strings with every escape, numbers with `_`,
  hexadecimal floats, `inf`, `nan`, `nan:0x...`; `;;` and nested `(; ;)` comments; annotations `(@...)` skipped) and the S-expression
  reader.
- `arbace.repr.wasm.text`: a module's fields into the items of the binary decoder (one vocabulary: WAS2 (a)), identifiers resolved
  (two passes: the index spaces first, with the text's rule that imports precede definitions), the abbreviations expanded (inline
  imports and exports, inline type uses with implicit types appended in order of first use, folded instructions, named params and
  locals, labels by name, a table's inline elements and a memory's inline data, the MVP's element and data spellings), numbers by the
  spec's ranges (`constant out of range`), GC's types and field names, the data count section when `memory.init` or `data.drop` is used.
- `arbace.repr.wasm.print`: the canonical text printer (flat, numeric indices, explicit type uses, explicit memargs, hexadecimal
  floats, data with `\hh` escapes).
- `arbace.repr.wasm.wast` and `bin/repr-wasm wast [--record] [--only LIST]`: each command of the testsuite's root scripts judged at the
  read level by its own assertion (doc/wasm/SPEC.md §8), with a print row for each module that reads; the negative controls of
  `test/wasm/controls-wast.txt` (an assert_malformed's module judged as a module, a module judged as malformed, two modules' forms).
- The REAP row `:wasm` ("WebAssembly", after the asm rows; WAS1 (a)): read (the Go-built round trip and the testsuite's read rows),
  print (the round trip's :full and the testsuite's print rows), eval and AOT "no check yet" (WA4, WA7); `test/crops/crops.edn`'s crop
  and `test/crops/independence.edn`'s stages.

**The testsuite at the read and print levels** (`bin/repr-wasm wast`, the testsuite's 258 root scripts; recorded in
`test/wasm/wast-results.edn`, classes `test/wasm/divergences-wast.edn`): **7,182 read cases, 4,459 pass, 2,723 classed, none failing**:
the 2,514 modules (text, `binary`, `quote`, `definition`, and those of assert_unlinkable, assert_uninstantiable, assert_trap,
assert_exception) all read, both formats agreeing (encoded and decoded again to the same forms; the binary ones printed back byte for
byte); the 1,945 `assert_malformed` all rejected (binary and quote); the 2,723 `assert_invalid` modules all read, their rejection the
validator's (`:validation`, `:deferred`, WA3). The print level: **2,514 of 2,514 modules' canonical text read again to the same
forms** (`print/...` rows). The controls (`test/wasm/controls-wast.txt`, six: assert_malformed modules judged as modules, modules
judged as malformed, two modules' forms compared) fail as they must. Measured (stage 0, by hand, one run): the whole 362 s and 0.58
GB; the essential subset (`test/wasm/essential-wast.txt`, 36 scripts, 2,195 cases, with `ops check`) 62 s and 0.49 GB.

The first whole run's 44 failures, fixed on the way (each a rule of the reference's parser): named params in block types and
`call_indirect` (`unexpected token`); any import after any definition (parser.mly's `module_fields`: `import after ... definition`);
duplicate field names; `unknown type` for an explicit type use with inline params whose index has no definition; 3.0's token rule
(a run of idchars, strings and `, ; [ ] { }` is one token, so `"a""b"` and `$"l"0` are reserved tokens: `unknown operator`); `(@"...")`
annotation ids and `(@` with no id inside an annotation; u64 memarg offsets and alignments and u64 limits printed unsigned; an empty
section that a custom section is placed after (written, not a layer fact); `float` casts of infinities (Arbace's `float` rejects
them: `.floatValue`).

**The row** (`bin/reap`): WebAssembly read 4,479 / 0 / 2,723 (pass / by design / deferred), print 2,534 / 0 / 0, eval and AOT "no
check yet" (WA4, WA7); `bin/reap` 0 problems (51 of 56 cells checked); `bin/crops-score --judge --strict` 0 problems, WebAssembly's
completeness 40.5%, maturity 83.3% (the fragment counted half), independence 60.0% (eval and AOT 0 until built).

**Not done in WA2 yet**: mode `:full` for the text (byte for byte, the identifiers and spellings as a layer), custom sections as
`(@custom ...)` annotations and names as `(@name ...)` (the `custom/` scripts), the `proposals/` and `legacy/` scripts (28 and 4,
read by `--only` but not in the record), the messages' second level (WAS8 (a): the reason's prefix against the script's). WA3 (the
validator) takes the 2,723 next.

## As built: WA2 (rest) (2026-11-13, branch `wasm-wa3`)

The user's decisions of 2026-11-13: the text's `$id`s stay dropped (into mode :full's layer later); the name section on `:name` stays;
the gate fragment is spliced when the branch merges (a key window). Built from `mac/wasm-wa1` (`ec5f2689f`).

- **The scripts beyond the root** (WAS5 (a)): `bin/repr-wasm wast` reads all 293 scripts by default, the 258 root ones and then
  `proposals/` (28), `legacy/` (4) and `custom/` (3); a non-pass's reason in those is prefixed by what its script tests
  (`arbace.repr.wasm.wast/tag-of`: `[proposal NAME]`, `[legacy exceptions]`, `[custom FILE]`) and classed by it in
  `test/wasm/divergences-wast.edn`: the six proposals beyond Wasm 3.0 deferred by WAS4 (a) (threads by WAS26 (a)), the legacy
  exceptions with WA8, the branch hints' annotations not built. Over the 35: **1,092 read cases, 638 pass, 454 classed** (threads 73,
  custom-descriptors 215, custom-page-sizes 42, compact-import-section 28, wide-arithmetic 10, extended-name-section 69, legacy 14,
  branch hints 3); the print level 246 of 246.
- **Annotations** (`custom/custom_annot.wast`, `custom/name_annot.wast`): the lexer keeps `(@custom ...)` and `(@name ...)` as
  annotation nodes (any other annotation still skipped); `arbace.repr.wasm.text` reads them as the reference's custom handlers do
  (`interpreter/custom/handler_custom.ml`, `handler_name.ml` at wg-3.0): a custom section's name, placement (`(after S)` kept as
  written, `(before S)` and the default after the last present section before it or of all) and bytes, with the handler's messages
  (`@custom annotation: missing section name`, `malformed placement`, `malformed section kind`, `unexpected token`, `misplaced @custom
  annotation`); the module's name (before every field) and each defined function's (before its type use) into the name section
  (`wasm/names`, `:name`), `@name annotation: multiple module names`, `misplaced @name annotation`, `local names not yet supported`. A
  tag's `@name` is accepted and not kept (the extended name section's subsection, `proposals/extended-name-section`). The canonical
  printer writes the custom sections back as `(@custom "N" (after S) "...")` and the names as `(@name ...)`, and the print level
  compares them (but an imported function's or a local's name, which the annotations cannot say). `assert_malformed_custom` is
  judged as `assert_malformed`; `assert_invalid_custom` (the custom sections' own checks) is not built (branch hints).
- **The messages' second level** (WAS8 (a)): each rejection's reason against the script's message by prefix, the reference runner's
  rule, as rows `msg/PATH#K` beside the read cell (`test/reap/cells.edn`'s `:role :beside` source). Over the root scripts **4,668 of
  4,668** after aligning the decoder and the text reader with the reference (and, where the testsuite's pin is newer than the
  interpreter's wg-3.0, with the testsuite: WAS27): lexer.mll's `unknown operator` for any keyword its table does not know and any
  reserved token (the keyword table generated into `arbace/repr/wasm/ops.ae` from lexer.mll's `keyword as s` rule, `ops gen`), its
  `misplaced control character`, `misplaced unicode character`, `illegal character` inside an annotation, `empty identifier` and
  `empty annotation id` for a `$` or `(@` with no well-formed string; decode.ml's stream not bounded by a section's or a body's size (a
  read past it fails as the module's stream does, the size checked after), its `len32` against what remains before the LEB128, its
  `either` for heap types (`integer representation too long`), the function and code counts compared after the last section; the
  text's `import after function definition` for the import's nearest definition, `wrong number of lane literals` and `... lane
  indices`, `i8 constant out of range` for a lane, `alignment must be a power of two`, `malformed UTF-8 codepoint`. Over the 35 scripts
  beyond the root 330 of 392, the rest in the same classes.
- Two text reader fixes the validator found: a struct's field ids counted after a `(field t t)` of several types (`struct.wast`), an
  implicit type use not matched to a member of a recursive group of more than one (`type-rec.wast`); and a table's inline elements
  typed as the table (parser.mly: function indices as `ref.func` expressions unless the type is `(ref func)`).

## As built: WA3 (2026-11-13, branch `wasm-wa3`)

**What was built** (doc/wasm/SPEC.md §9): `arbace.wasm.valid` (`arbace/wasm/valid.ae`, a crop path since WA1), Wasm 3.0's validation
as the reference interpreter's `interpreter/valid/valid.ml` and `match.ml` state it at the spec's pin: the recursive type groups with
their subtype checks, GC's matching (iso-recursive equivalence by canonical forms, declared supertypes), the imports, tags, functions,
memories, tables with their initializers (a table without one initialized with `ref.null`), globals, data and element segments,
constant expressions, then every body by the algorithm of the spec's appendix (an operand stack with an ellipsis and `bot`, labels, the
locals' initialization), the start function, the exports and their names; every one of the table's 493 instructions typed (the
numeric and vector ones by their names' shapes, the rest one by one), with valid.ml's messages. Three rules follow the testsuite's pin
where it is newer than wg-3.0's interpreter (WAS27): at most one supertype (`multiple supertypes`), a memory access's offset bound by
the accessed memory's address type (wg-3.0 reads memory 0's), and the words `immutable array`, `immutable field`.

**The judges**: `bin/repr-wasm wast` validates every module that must read (a valid module rejected fails: `valid module rejected:
MESSAGE (function I)`) and judges every `assert_invalid` by its own assertion, rejected (WAS9 (a): read's acceptance), the message the
second level; `bin/repr-wasm roundtrip` validates each Go-built module first (`invalid` a status). The negative controls
(`test/wasm/controls-wast.txt`, four more, ten in all): an assert_invalid's module judged as one that must validate (two), a valid
module judged as one the validator must reject, an assert_invalid's rejection against another assertion's message: each fails as it
must.

**Counts** (before: WA2's record; after: this branch's, `test/wasm/wast-results.edn`):

| | before (WA2) | after (WA3) |
|---|---|---|
| root scripts, read cases | 7,182: 4,459 pass, 2,723 `:validation` | 7,182: **7,182 pass** |
| root scripts, messages (msg/) | not judged | **4,668 of 4,668** |
| root scripts, print | 2,514 of 2,514 | 2,514 of 2,514 (with customs and names) |
| proposals/, legacy/, custom/ | not in the record | 1,092 read cases: 638 pass, 454 classed; messages 330 of 392; print 246 of 246 |
| Go-built modules (round trip) | 20 of 20 | 20 of 20, each validated |
| REAP read cell | 4,479 / 0 / 2,723 | 7,840 / 0 / 454 |
| REAP print cell | 2,534 / 0 / 0 | 2,780 / 0 / 0 |

The first whole run with the validator: 7,174 of 7,182 (eight: the struct field ids, the rec group's implicit type, the inline
elements' type, the offset's memory, multiple supertypes); the messages 4,198 of 4,663 before the alignment above. `bin/reap` 0
problems; `bin/crops-score --judge --strict` 0 problems, WebAssembly's completeness 40.5% → 48.6%, maturity 83.3%, independence
60.0%. Measured (stage 0, by hand, one run at a time): `bin/repr-wasm wast` over the 293 scripts 441 s and 0.60 GB; the essential
command (`ops check` and 40 scripts, `test/wasm/essential-wast.txt` with type-subtyping, unreached-invalid and the two annotation
scripts) 101 s and 0.53 GB; the round trip 1,115 s and 1.56 GB (validation adds about 300 s). The branch moves no key beyond WA1's
(`bin/arbace-go --gate-key` `dbf88e978ca5...` before and after; `gate-key-crops-test` passes).

**Go 1.27.1's SIMD encoding, verified** (the brief: verify only, no upstream report). WA1's reading of
`cmd/internal/obj/wasm/wasmobj.go`'s `writeOpcode` was that the two SIMD ranges at or above 0x80 write a byte after the opcode's
u32. A probe (`.tmp/simd/main.go` in the worktree: `simd/archsimd`'s `Int8x16.Add` (0x6e), `Int16x8.Abs` (0x80), `Int32x4.Add` (0xae),
`Int32x4.Mul` (0xb5), `Float32x4.Add` (0xe4), `Float64x2.Mul` (0xf2)) built by TamaGo's go1.27.1 for wasip1 with
`GOEXPERIMENT=simd` (2,476,196 bytes) and read by repr/wasm: it decodes, round-trips byte for byte and validates, and each of the five
instructions at or above 0x80 is followed by a `nop` (the stray 0x01: `writeUleb128` already writes the u32's two bytes), the one below
0x80 is not. So the finding holds as a size and fidelity defect that happens to stay valid: the stray byte is an opcode. For the
relaxed range the same code writes `writeUleb128(op - 0x100 + 0x80)` and 0x02, so a relaxed instruction would decode as the
instruction 0x80 below its opcode (i16x8.abs ...), a `nop`, then 0x02 (`block`): wrong code, by reading; `archsimd` exposes no relaxed
operation, so no build shows it. Nothing reported upstream.

**Proposals** (numbered after WAS26):

- **WAS27 The messages' reference.** Where the testsuite's pin (92a3d806) and the spec's interpreter (wg-3.0) disagree, (a) the
  testsuite's assertions decide (done: six rules above), the interpreter read for the rest; (b) wg-3.0's interpreter, the testsuite's
  newer cases classed. *Recommended: (a)*: the testsuite is the judge.
- **WAS28 A tag's name.** (a) accepted and dropped until the extended name section (its subsection 11), with that proposal; (b) the
  extended name section's subsections read now (types, fields, tags, labels, segments). *Recommended: (a)*.
- **WAS29 Branch hints.** (a) the `@metadata.code.branch_hint` annotations and their custom section with WA8; (b) now. *Recommended: (a)*.

**Not done**: the legacy exceptions (WA8), the proposals beyond 3.0 (WAS4 (a)), mode `:full`'s text (byte for byte, the `$id`s and
spellings as a layer: the user's "later"; it does not fit this branch beside WA3), the branch hints.

## As built: WA4 (2026-11-13, branch `wasm-wa4`)

The user's decisions of 2026-11-13: WAS27-WAS29 as recommended, and WA4 started. Built from `mac/wasm-wa3` (`6da9f9be2`). doc/wasm/WAI.md
describes the engine, its internal code, its hosts and the check.

**What was built**:

- **`go/arbace/wai`** (WAS6 (a), WAS7 (a); a crop path since WA1, so no key moves): the decoder of the binary format (every section of
  Wasm 3.0, GC's rec groups canonicalized for the process: iso-recursive equivalence as id equality, subtyping by the declared
  supertypes), the translation of each body at load into a flat internal code (static stack heights: a branch carries its arity, its
  target's height and pc; `try_table`'s clauses as handlers over pc ranges), the store, the linker (match.ml's rules: `unknown import`,
  `incompatible import type`), instantiation in eval.ml's order, the interpreter (calls as Go calls under a depth of 20,000, tail calls
  in place, traps as panics recovered at the boundary, exceptions looked up in the frames' handlers), SIMD128 and relaxed SIMD (each
  lane written out; the relaxed choices the reference interpreter's, `relaxed_madd` fused and rounded once), GC's structs, arrays,
  `i31`, casts and conversions, and the `spectest` host. 5,100 lines of Go written in a module copy (`.tmp/wai-src`), converted to forms
  by repr/go's native reader (`bin/repr-go read --goos linux --goarch amd64`, a package at a time, about 75 s and 1.1 GB each under
  GOMEMLIMIT=1100MiB GOGC=25 REPR_GO_JOBS=1: the driver's first conversion at the defaults passed 2.5 GiB and was killed by the watcher),
  224 KB of forms; the op table `optab` generated from repr/wasm's `ops.ae` (`bin/wai ops gen|check`, `test/wasm/gen_wai_ops.awk`).
  wai has no validator of its own yet (WAS30).
- **The light driver** `test/wasm/wai-driver` (`waidriver wast|control|ops`), built with wai by TamaGo's go alone (`bin/wai light`:
  `bin/build-go --print-only`, then `go build`, seconds; kept by its sources' digest).
- **`arbace.wasm.check`** (`arbace/wasm/check.ae`): the planner, on the tools' host: each script read by repr/wasm, its modules written
  as binaries (a `binary` one's own bytes, the rest encoded canonically), each command a line of the plan with its arguments and
  expected results as tokens (doc/wasm/WAI.md §5.1-§5.2).
- **`bin/wai`** (`light`, `check [--record] [--only LIST]`, `essential`, `divergences`, `ops gen|check`; Makefile `wai-*`): the plan, the
  run, the judgment against `test/wasm/eval-results.edn` and `test/wasm/divergences-eval.edn`, the controls of
  `test/wasm/controls-eval.txt`. The judges are the reference runner's (WAS8 (a)): `assert_return` by `assert_result` (the NaN
  patterns by their bits, references by kind, `either`), `assert_trap`, `assert_exhaustion`, `assert_exception`, `assert_unlinkable`,
  `assert_uninstantiable` (and `assert_trap` of a module), `register`, `get`, `invoke` across modules; each trap's and link failure's
  message a `msg/` row by prefix (the second level). Beyond the root, a non-pass's reason is prefixed by its proposal as
  `bin/repr-wasm wast`'s are, and classed by it.
- **The row**: the eval cell's sources (`test/reap/cells.edn`: the commands, and the messages `:role :beside`); `test/crops/crops.edn`'s
  controls, `test/crops/independence.edn`'s eval stage (1: wai, built by the seed's toolchain); the fragment `test/gate/crops/wasm.edn`
  gains `wai-essential` (essential, chain `wasm-essential`) and `wai-check` (`--full`), not spliced (as WA1-WA3's steps: at the merge).
- **A reader fix the run found**: repr/wasm's lexer ended a `;;` comment at U+0A alone; a newline is also U+0D (comments.wast's
  `module quote` with `;; comment\0d`: the read and print levels passed, the forms read the same wrong way both times; wai's result
  1 for 2 showed it).

**Counts** (`bin/wai check --record`):

| | before (WA3) | after (WA4) |
|---|---|---|
| root scripts, eval commands | no check | **60,498 of 60,498 pass** (modules, actions and every assertion of eval: the planner writes no line for assert_malformed and assert_invalid, the read cell's) |
| root scripts, messages (msg/) | no check | **5,192 of 5,192** |
| proposals/, legacy/, custom/ | no check | 1,437 commands: 395 pass, 1,042 classed (threads 259, custom-descriptors 496, wide-arithmetic 101, custom-page-sizes 54, compact-import-section 45, extended-name-section 12, legacy exceptions 75); messages 65 of 66 (1 threads) |
| REAP eval cell | no check yet | 60,893 / 0 / 1,042 |
| controls | | 12 fail as they must, 3 positive checks pass |

The first whole run (a dev build from the module copy, before SIMD): 36,370 pass, 25,697 fail, all but four `unimplemented: simd`
(the four: a canonical-type key whose separator was the byte 0x7c, f64's code, so `(func (param f64))` and `(func (result f64))`
collided; an element trap's message without its index; comments.wast). That run's first try also exposed a growth overflow:
`table.grow` on a table64 by 2^64-1 wrapped `old+n` and appended without end, 55 GB before the kernel killed it (a rule slip: the
driver ran without a watcher; every run after under `.tmp/watch.sh`'s 2.5 GiB). With SIMD and relaxed SIMD: one failure, an f32
`relaxed_madd` rounded twice (through float64), fixed by rounding the float64 sum to odd. Measured (stage 0 and TamaGo's go, by hand,
one run at a time): the check over the 258 root scripts 263 s and 0.68 GB (the 293: 0.62 GB), almost all of it the planner on stage 0, the run on wai about 2.5 s; the essential command
(`bin/wai ops check && bin/wai essential`, 53 scripts, 18,601 commands) 62 s and 0.54 GB.

**Proposals** (numbered after WAS29):

- **WAS30 wai's own validator.** (a) a validator in wai, judged against arbace.wasm.valid's verdicts over the testsuite as a `:beside`
  source (the survey's §13.1), before WA9 runs modules Arbace did not validate; (b) none: wai runs only what Arbace validated.
  *Recommended: (a)*, with WA5 (the Go-built modules are its first inputs not from the testsuite).
- **WAS31 SIMD brought forward.** WA6's SIMD128 and relaxed SIMD were built here (Wasm 2.0 includes SIMD128: the brief's "2.0's
  semantics"). (a) keep WA6 for what is left of it (the Go-built SIMD programs with `GOEXPERIMENT=simd`, a SIMD corpus beyond the
  testsuite); (b) fold WA6 into WA4. *Recommended: (a)*.
- **WAS32 The proposals' scripts at eval.** (a) as built: the 35 scripts beyond the root run and classed by proposal, as at read; (b)
  only the root scripts until WA8. *Recommended: (a)*.

**Not done**: WASI preview 1 and the Go-built `wasip1` programs (WA5; WAS11's start did not fit), wai's own validator (WAS30), the legacy
exceptions and the proposals beyond 3.0, the AOT cell (WA7).

## As built: WA5 (2026-11-13, branch `wasm-wa5`)

The user's decisions of 2026-11-13: WAS30 (a) (wai gets a validator of its own, judged against arbace.wasm.valid as a `:beside`
source), WAS31 (a) (SIMD stays in WA4; WA6 keeps Go's SIMD programs and a SIMD corpus beyond the testsuite), WAS32 (a) (the 35
scripts beyond the root stay run and classed by proposal), and WA5 started. Built from `mac/wasm-wa4` (`d673e51c9`). doc/wasm/WAI.md
§6-§8 describe the new parts.

**What was built**:

- **wai's validator** (`go/arbace/wai/valid.go`, WAS30 (a); doc/wasm/WAI.md §7): Wasm 3.0's typing rules over the binary, on its own
  pass (the decoder still assumes a valid module), after the specification's validation algorithm and valid.ml as arbace.wasm.valid
  follows them: rec groups and subtyping on the decoder's canonical types, limits, the index spaces, constant expressions, the bodies
  over an operand stack with an unknown bottom and a control stack (try_table's clauses, the non-defaultable locals' initialization,
  undeclared `ref.func`), the numeric and vector signatures by name. The driver validates every module before it decodes it, so the
  eval cell's commands now run only modules wai's own validator accepts.
- **Its judgment**: the planner (`arbace.wasm.check`) writes, for every module it writes as a binary and for every `assert_invalid`'s
  module (encoded canonically, as the eval modules are), arbace.wasm.valid's verdict (`K valid FILE ok|invalid:MSG|none:REASON`); the
  driver's `valid/SCRIPT#K` row passes when the two validators agree. Its controls (`valid-flip`, `valid-self`) in
  `test/wasm/controls-eval.txt`.
- **WASI preview 1** (`go/arbace/wai/wasi.go`; doc/wasm/WAI.md §6): the 36 names Go's wasip1 port imports (the survey's "38" counted
  two names declared twice: `fd_write` and `clock_time_get` in both runtime and syscall) and the rest of the snapshot's, over
  preopened directories resolved component by component inside their preopen (no `..` above it, symbolic links followed inside it,
  absolute targets refused), read-only preopens refusing every change; `proc_exit` an `*Exit` recovered at the engine's boundary;
  `random_get` seeded; `poll_oneoff`'s clocks sleep until the earliest. The driver's `run` (WASI command: validate, decode, link,
  `_start`) and `buildok` (Go's test driver's build-constraint rule for wasip1/wasm, cmd/internal/testdir's `shouldTest`).
- **The Go run corpus** (`bin/wai go`, `bin/lib/wai/go-run.bash`, WAS11 (a); doc/wasm/WAI.md §8): `$GOROOT/test`'s run programs (the
  ids of goi's record whose action is `run`: 1,051) and Go's own small programs of `test/wasm/go-run.txt` (hello, goroutines, maps,
  fmt, time, files: `test/wasm/go/NAME/main.go` with `want.out`, the first five's equal to gc's linux run, files' a sandbox test),
  built for wasip1 by TamaGo's go1.27.1 (cached by key in the main checkout's `.tmp/wai-go`), run on wai with `$GOROOT` preopened
  read-only and `$GOROOT/test` the working directory, judged as goi's check judges goi (gc's linux/amd64 run of the original, goi's own
  references read by goi's own key; status, standard output, standard error's head); the record `test/wasm/go-results.edn`, the
  classes `test/wasm/divergences-go.edn`, the controls `test/wasm/controls-go.txt` (4 negative, 2 positive), the essential list
  `test/wasm/essential-go.txt` (the own programs, every fifteenth of goi's essential run programs, the controls' subjects: 44).
- **The row and the gate**: the eval cell's sources gain `bin/wai go` (programs) and the validator's rows (`:role :beside`); the
  crop's controls and independence (eval and judge stages name the new steps); the fragment gains `wai-go-essential` (essential,
  chain `wasm-essential`) and `wai-go` (`--full`), not spliced; Makefile `wai-go`, `wai-go-essential`, `wai-go-record`.

**Counts**:

| | before (WA4) | after (WA5) |
|---|---|---|
| root scripts, eval commands | 60,498 of 60,498 | **60,498 of 60,498** (every module now validated by wai first) |
| root messages (msg/) | 5,192 of 5,192 | 5,192 of 5,192 |
| wai's validator against arbace.wasm.valid, root (valid/) | none | **5,237 of 5,237 agree** (2,514 accepted by both, 2,723 rejected by both, the assert_invalid's) |
| the same beyond the root | none | 567: 341 agree, 226 classed by proposal (all modules arbace.wasm.valid gives no verdict on: they do not read or encode) |
| beyond the root, eval commands | 395 pass, 1,042 classed | 391 pass, 1,046 classed: 4 more in custom-descriptors (wai's validator now rejects, as arbace.wasm.valid does, two br_on_cast modules of exact types; two exact.wast modules) |
| the Go run corpus | none | **1,057 programs: 1,001 pass**, 53 :na (Go's test driver would not run them on wasip1/wasm), 3 over budget (`finprofiled.go`, `fixedbugs/issue78081.go`, `fixedbugs/issue79186.go`: past three times the action's limit) |
| of them goi's essential run programs | none | 533: 500 pass, the rest :na or over budget |
| controls | 12 negative, 3 positive | eval 16 negative, 5 positive; Go 4 negative, 2 positive: all as they must |
| REAP eval cell | 60,893 / 0 / 1,042 | **61,890 / 0 / 1,046** (3 over budget in the denominator; 53 n/a beside) |

The first whole Go run (before the build-constraint rule): 1,026 of 1,057 passed, every failure but five a program Go's test driver
would not run on wasip1/wasm (it runs the go command, needs `syscall.Mmap`, or states `!wasm`); the five: `cmplxdivide.go`'s action
names a second file (`run cmplxdivide1.go`: built with it since, as goi's forms' `:files` list it), `winbatch.go` reads
`$GOROOT/src/all.bat` (the preopen widened from `$GOROOT/test` to `$GOROOT`, read-only), and the three over budget. The testsuite's
first run with the validator: 57 + 31 root commands refused for "data count section required" (Arbace's canonical encoder writes no
data count section, WAS33), five element segments typed `(ref null func)` where Wasm 3.0's binary format gives a segment of function
indices `(ref func)`, and one heap index 2^31 read as a negative abstract type; fixed.

Measured (one run at a time, under a 2.5 GiB watcher): the testsuite's check about 370 s and 0.69 GB (the planner now also running
arbace.wasm.valid over every module), the essential command 107 s and 0.54 GB (with the driver's rebuild); the Go corpus cold 17.7
minutes and 1.86 GB summed over three jobs (the builds the cost), warm 9 minutes; `bin/wai go --essential` warm 6 s and 0.19 GB (cold,
its 44 builds, about a minute). The conversions by repr/go's native reader: wai 123 s and 1.05 GB, the driver 64 s and 1.02 GB. The
branch moves no key (gate-key-crops-test passes; `bin/arbace-go --gate-key` equal on `wasm-wa4` and `wasm-wa5`).

**Proposals** (numbered after WAS32):

- **WAS33 The data count section.** Arbace's canonical encoder (`arbace.repr.wasm.binary/encode`) writes no data count section, which
  the binary format requires where a body uses a data index (`memory.init`, `data.drop`, `array.new_data`, `array.init_data`); the
  testsuite's modules so encoded are malformed by the letter. (a) the encoder writes it when a body uses a data index (repr/wasm's round
  trips and records rerun); (b) as built: wai's validator bounds the data indices by the data section's count when the section is
  absent. *Recommended: (a)*, then wai's validator strict again.
- **WAS34 The Go corpus's directory tests.** goi's record has 127 directory tests (rundir, buildrundir, runindir) beside its 1,051
  run programs. (a) build them for wasip1 as goi's check builds them (their packages as a GOPATH tree) in a WA5b; (b) leave them to
  WA6. *Recommended: (a)*.
- **WAS35 The speed limit.** (a) as built: three times the action's limit (`WAI_GO_SLOW`), the programs past it `:budget` until w2g
  (WA7) gives the AOT level; (b) a timeouts file of wai's own, as goi's. *Recommended: (a)*.
- **WAS36 The validator's messages.** (a) as built: the verdicts alone (both accept or both reject); (b) a second level, wai's
  message against arbace.wasm.valid's by prefix. *Recommended: (a)*: the testsuite's messages are the read cell's second level
  already.
- **WAS37 `wai-go-essential` in the essential gate.** (a) as proposed in the fragment: 44 programs, about a minute cold and seconds
  warm (builds cached by key in the main checkout); (b) `--full` only. *Recommended: (a)*.

**Not done**: the directory tests (WAS34), Go's js/wasm programs and the headless world (WA11), `wasmexport.go` and
`wasmexport2.go` (errorcheck programs: gc's errors for wasm, not runs), `wasmmemsize.go` (a runindir test: WAS34's), the AOT cell (WA7), the legacy exceptions (WA8).

## As built: WAS33, WA5b, WA7 (first part) (2026-11-13, branch `wasm-wa6`)

The user's decisions of 2026-11-13: WAS33-WAS37 all (a). Built from `mac/wasm-wa5` (`114e8b256`). doc/wasm/WAI.md §8 (the directory
tests) and §9 (w2g) describe the new parts.

**WAS33 (a), the data count section.** `arbace.repr.wasm.binary/encode` writes the data count section whenever a function's body
names a data segment and the forms carry none (encode.ml's rule: `Free.module_`'s datas, the data segments' count, zero when there
are none); the text reader (`text.ae`) adds the `wasm/datacount` item by the same rule (before: only for `memory.init` and
`data.drop`, and only with data segments: `array.new_data` and `array.init_data` were missed, and `(data.drop 0)` with no segment);
the decoder requires the section for all four instructions, as decode.ml does (before: two). wai's validator is strict again: a body
naming a data segment without the section is malformed ("data count section required"), its WA5 leniency (the data section's own
count) removed. Proofs: `bin/repr-wasm wast` over the 293 scripts unchanged (the record holds: 7,820 of 8,274, the root's all; 2,760
of 2,760 print cases; 4,998 of 5,060 messages), `bin/wai check` unchanged (60,498 of 60,498 root commands, the 5,237 root modules'
validator rows all agreeing, now with no lenient case); the round trip over every corpus (`bin/repr-wasm roundtrip`: go-wasip1
and go-js) 20 of 20 modules byte for byte, 1,070 s and 1.56 GB. Its first two runs were killed by the watcher at 2.6 GB on
`map.wasm`: the encoder's first test of a body's data indices walked every function through `tree-seq`, which cost more than the whole
round trip (`map.wasm` alone over 2.6 GB in 216 s, against 1.48 GB in 50 s without the test); a walk of the instructions' lists and
vectors that stops at the first (`uses-data?`) brought it back to 1.48 GB and 55 s.

**WA5b (WAS34 (a)), the directory tests.** `bin/wai go` builds goi's record's 127 directory tests and the four of `$GOROOT/test`'s
that goi's linux record does not hold (`test/wasm/go-dirs.txt`: `wasmmemsize.go`, `//go:build wasm`, and three that skip) for
wasip1 as cmd/internal/testdir builds them (`bin/lib/wai/go-run.bash`'s `gorun_dir_build`): rundir, each package of the `.dir` in the
files' order compiled by `go tool compile -e -D test -p test/NAME` against the standard library's export data for wasip1 (`go list
-export std`, once a run), the last linked as main by `go tool link -s -w -buildid=test`; buildrundir, every `.go` compiled as main
with the `.s` files assembled (`-gensymabis`, `-asmhdr`), packed and linked; runindir, a shallow copy made a module named after the
directory (`-gomodversion`'s go line), `go build .`; cached by the directory's files' digest. Run by wai in Go's driver's directory
(rundir's and buildrundir's a fresh work directory, runindir's the module's copy, preopened beside `$GOROOT` read-only), standard
output and error combined, judged as Go's test driver and goi's check judge them: status 0 and the output the golden `.out` byte for
byte (none: no output). Two negative controls and two positive checks of the new mode `golden` (`test/wasm/controls-go.txt`); five
directory tests added to the essential list (`wasmmemsize.go` among them).

**WA7 (first part), w2g** (WAS10 (a); doc/wasm/WAI.md §9): wai's internal code translated to Go and built by TamaGo's go.

- **The unit and the bodies.** w2g translates wai's internal code, not the binary: its static stack heights (`Code.H`, recorded by
  the compiler since WA7) make each operand a slot of the frame known at translation, and its branches carry their targets. A
  function becomes one Go function over the machine's stack (the interpreter's calling convention): a label a branch target, a
  `goto` a branch, `br_table` a `switch`, calls through `Machine.run`; every instruction that is not a transfer of control is the
  interpreter's own case of `Machine.exec`, generated from `exec.go` (`waidriver w2g-cases`; `bin/wai w2g gen|check`), a case
  shared by several ops reduced to the op's own clause and the instruction's fields made constants (those that hold the process's
  canonical type ids read at run time). So the AOT level runs the interpreter's semantics instruction for instruction (WAS39).
- **Tail calls and handlers.** `return_call*` leaves its callee to `Machine.run`'s trampoline (the callee runs in the same frame once
  the caller returned: neither Go's stack nor the depth grows; the interpreter tail-calling a translated function does the same); a
  function with `try_table`'s handlers sends every exception raised inside it to a label that asks `Machine.catch` at the raising
  instruction, as the interpreter's `throw:` does. The first runs left them to the interpreter (185 root commands); since then every
  function of the testsuite's 2,000 modules and of the Go programs is translated.
- **The programs.** `waidriver w2g OUTDIR MODULE...` writes packages `w2gout/pN` of about 1.5 MB of Go each, which dot-import wai
  (its names the bodies use and wai does not export are wrapped as `X_NAME` by the generated `w2gexports.go`), each registering its
  functions by their module's SHA-256; an AOT program is the light driver built by TamaGo's go with these packages
  (`bin/lib/wai/aot.bash`), its translated functions attached at instantiation. One package was the first try: a Go-built module
  (`hello.wasm`, 2.5 MB, 1,624 functions, 646,000 instructions) is 52 MB of Go, and its one compilation passed the watcher's 2.5 GiB
  in 82 s; in 37 packages compiled one at a time it builds in 134 s at 0.84 GB.
- **The judge.** The same plans and judges as eval, and one rule more: `Machine.Interpreted` counts the Wasm calls the interpreter
  ran in an AOT program, and a command (or a Go program's run) that passes having counted any is no AOT pass ("w2g: N calls on the
  interpreter (REASON)", class `:w2g-left`, none now). Its controls (`test/wasm/controls-aot.txt`): the eval controls' modes run by
  the AOT programs (11 negative, 4 positive) and `as-interpreted`, three commands run by their AOT program with its translated
  functions detached (`AOTDisable`), which the AOT judge must refuse and does.
- **`bin/wai check --aot`** (the testsuite, 13 programs of 24 scripts) and **`bin/wai go --aot`** (`test/wasm/go-aot.txt`), their
  records `test/wasm/aot-results.edn` and `test/wasm/go-aot-results.edn`, the classes `test/wasm/divergences-aot.edn` (w2g's own,
  then eval's by proposal); the REAP AOT cell's sources (commands; messages beside; the Go programs); the fragment gains
  `wasm-aot-essential` (essential: `bin/wai w2g check && bin/wai aot-essential`, `test/wasm/essential-aot.txt`'s 31 scripts) and
  `wai-aot`, `wai-go-aot` (`--full`), not spliced (WAS37 (a): the fragment's `wai-go-essential` was already essential; spliced at
  the merge).

**Counts**:

| | before (WA5) | after (WA6 branch) |
|---|---|---|
| root scripts, eval commands | 60,498 of 60,498 | **60,498 of 60,498** (the strict validator: unchanged) |
| root scripts, AOT commands (w2g) | no check | **60,498 of 60,498** (every function of the 2,000 modules translated: 7,456 of 7,456) |
| root messages at AOT | no check | **5,257 of 5,258** pass (the one left: threads) |
| beyond the root, AOT | no check | 391 pass, 1,046 classed by proposal: eval's, row for row |
| AOT controls | none | 14 negative (3 `as-interpreted`), 4 positive: all as they must |
| the Go run corpus (eval) | 1,057 programs: 1,001 pass, 53 :na, 3 :budget | **1,188 programs: 1,124 pass**, 59 :na, 2 :na (actions goi skips), 3 :budget |
| of them directory tests | none | **131: 123 pass** (122 of goi's 127, `wasmmemsize.go`), 6 :na (build constraints), 2 :na (`-goexperiment`) |
| the Go programs at AOT (`go-aot.txt`) | none | **18 of 18 pass**, the 3 over eval's budget among them (`finprofiled.go`, `fixedbugs/issue78081.go`, `fixedbugs/issue79186.go`) |
| REAP AOT cell | no check yet | **60,907 / 0 / 1,046** (the testsuite's 60,889 and the 18 Go programs; eval's cell 62,013 / 0 / 1,046) |
| crops-score, WebAssembly | completeness 0.7322, maturity 0.8750, independence 0.8000 | **0.9779, 0.9167, 1.0000** |

**Measured** (one run at a time, under the 2.5 GiB watcher, while night 13's gate ran on next): `bin/wai check` 386-417 s, 0.69-0.71
GB; `bin/wai check --aot` 439-540 s, 0.62-1.0 GB (the planner as eval's, then 13 translations and builds of about 10 s each, the
runs); `bin/wai go` with the directory tests 712 s cold for the 131 new builds, 1.89 GB summed over three jobs; `bin/wai go --aot`
over 18 programs 3,454 s, 0.68 GB (a Go program's translation 2 s and 0.21 GB, its 37 packages built in about 2.5 minutes);
`bin/wai go --essential` warm 10 s; `bin/repr-wasm wast` 592 s, 0.61 GB. The conversions by repr/go's native reader: wai 94-140 s and
1.03-1.06 GB, the driver 99-140 s and 0.97 GB.

**Proposals** (numbered after WAS37):

- **WAS38 w2g's form.** (a) as built: w2g is wai's (Go forms, a light driver's code), translating wai's internal code and writing Go
  source in packages for TamaGo's go; (b) the survey's `arbace.wasm.w2g` on the tools' host, writing Go forms printed by repr/go. A
  Go-built module is about 50 MB of Go: as forms on stage 0 and through repr/go's printer that is hours a program, where (a) takes
  2 s. *Recommended: (a)*; (b) stays possible for small modules (the forms of a translation, when they are wanted as data).
- **WAS39 The bodies from the interpreter.** (a) as built: every non-control instruction is the interpreter's case, generated from
  exec.go (`bin/wai w2g check` in `wasm-aot-essential`); (b) templates of w2g's own per op. (a) keeps one semantics (an interpreter
  fix is w2g's at the next generation) and made the whole testsuite pass at AOT once the transfers of control were right; its cost is
  speed (the cases' slot accesses through the stack's slice, not registers). *Recommended: (a)*; a register allocation over the
  static heights is a later optimization, judged by the same records.
- **WAS40 The whole Go corpus at AOT.** About 3 minutes a program (the translation's 37 packages built), 1,130 programs: about 57
  hours. (a) `go-aot.txt` (18 programs: the own programs, the three over budget, the controls' subjects, two directory tests) as the
  `--full` step, and the rest as a rotation (GT8.4's `:rotate`, e.g. 100 programs a night, the whole corpus in about 11 nights); (b)
  only `go-aot.txt`; (c) translate a Go program's runtime and libraries once (their functions keyed by content, calls through the
  module's tables): much faster, but a translation keyed across programs is a design of its own (WA7's rest). *Recommended: (a)*,
  then (c) if the rotation's cost matters.
- **WAS41 The AOT judge's strictness.** (a) as built: a command or a program whose run called any function the interpreter ran is
  no AOT pass; (b) by module (a module some of whose functions were translated counts). *Recommended: (a)*: an AOT pass that
  interpreted is not one, and since this branch nothing is left.
- **WAS42 The AOT level's time limit.** (a) the Go programs at AOT keep eval's three times the action's limit (as built); (b) gc's own
  limit (one time), now that the code is compiled. *Recommended: (a)* until the rotation measures the whole corpus.

**Not done**: the whole Go corpus at AOT (WAS40), the forms of w2g's output (WAS38 (b)), a register allocation, the js/wasm programs and
the headless world (WA11), `wasmexport.go` and `wasmexport2.go` (errorcheck programs: gc's errors for wasm), the legacy exceptions
(WA8), Go's SIMD programs and WA6's SIMD corpus.

## As built: WA11, WA12 (begun) (2026-11-14, branch `wasm-wa7`)

Built from `mac/wasm-wa6` (`176a00065`). The user's focus for the crop: a minimal HTML and JavaScript shim, Wasm in the browser as the
single-page WebGPU games do it. doc/wasm/WAI.md §10 (the headless world) and doc/wasm/WEB.md (the shim, the bridge, the demo) describe
the new parts. Everything is in `arbace_crop_paths` or outside every key (`wasm/`, `test/wasm/`): no key moves.

**WA11, gojs: the headless world** (WAS13 (a), WAS16 (a), WAS17 (a)). Go's js/wasm import ABI in wai (`go/arbace/wai/gojs.ae`, written
in Go after the pinned TamaGo go1.27.1's `lib/wasm/wasm_exec.js` and `wasm_exec_node.js`, BSD-3, origin in LICENSE.md's Go section):
the 24 gojs imports and `debug`, each of Go's stack pointer at wasm_exec.js's offsets; the reference table (NaN-boxed ids, the seven
predefined, reference counts, SameValueZero interning, the pool); `run`, `resume`, `getsp`; `_gotest` (syscall/js's tests' imports);
re-entry (a `js.FuncOf` callback, an export called from an import) on the running machine, which needed one change to the engine:
`Machine.HostCall` runs a host function with the machine's base above its caller's operands (the interpreter's calls and w2g's
generated calls use it). Over it: the JS value model and runtime (`js.ae`, `jsrt.ae`: Object, Function, Array, the typed arrays,
Number, Boolean, String, Symbol, the errors, Date, Promise with microtasks, Math, console, performance, crypto, timers), a small
`eval` (`jseval.ae`, WAS43), Node's part (`gojsnode.ae`: `fs`'s callback API over the driver's mounts, `process`, `path`), the page's
part (`gojspage.ae`: `window`, `document` with one canvas, `navigator.gpu` and every GPU object a recording fake, requestAnimationFrame,
input events, the bridge's decoder) and the event loop on a virtual clock (WAS44). `waidriver gojs` runs a module as go_js_wasm_exec
runs it (Node's mode) or as a page (`--frames N --input FILE --record FRAMES.EDN`).

- **The exit**: syscall/js's tests (`go test -c syscall/js`, GOOS=js, by TamaGo's go; `-test.v`) on wai: **37 of 37 pass**, the binary's
  PASS (TestWasmExport's export called from an import and TestInterleavedFunctions' callbacks among them). Go's own js programs
  (`test/wasm/js/`: hello, timers, promise, files, callback, deadlock) **6 of 6** by their statements (`want.out`, `want.status`,
  `want.err`: the deadlock program's status 2 and Go's message, as wasm_exec_node.js's exit handler gets it).
- **The controls** (`test/wasm/controls-js.txt`): three programs judged against another's statement, the scene one frame off and without
  its key press, the shim without an import and with an argument count changed: **7 of 7 fail as they must**; two `self` checks pass.

**WA12 begun: the shim, the bridge, the demo** (WAS15 (a), WAS16 (a), WAS22 (a)).

- **The shim** (`wasm/web/index.html`, `wasm/web/arbace.js`, 526 lines): part 1 the gojs imports and the `Go` class from wasm_exec.js
  as they are; Arbace's own parts: a memory file system behind Node's API with standard input from the page's input line (WAS13 (a)),
  the frame loop and resize, the input ring, the WebGPU bridge, the start. **The import-list check** (`bin/wai shim-check`, rows
  `js/shim/imports` and `js/shim/gpu-ops`): the names of arbace.js's gojs object = the pinned Go's `//go:wasmimport gojs` directives (24,
  with `debug` 25) = wasm_exec.js's = the headless world's; the bridge's argument counts = the world's table. No JavaScript runs at check
  time (§10.3).
- **The bridge**: setup through syscall/js into the shim's handle table; each frame one command buffer in linear memory, 13 operations
  named after `webgpu.h` (doc/wasm/WEB.md §2), `QueueWriteBuffer` a view of linear memory; the input ring (§3). The Go side is the
  package `arbace/webgpu` (`wasm/demo/webgpu/`); the world's decoder is the shim's statement for statement over the fakes.
- **The demo** (`wasm/demo/scene/`): a lit rotating cube (a depth buffer, one render pass a frame) and a compute pass over 256 particles,
  the arrow keys changing the spin, a fixed 60 Hz step. Its 90 frames on the headless world with `input.txt` (ArrowRight before frame
  31) against `test/wasm/js/scene/expected/frames.edn`, the authors' statements from the scene's arithmetic (the projection; the model
  matrix at frames 1, 15, 30, 31, 45, 60, 90, within 1e-5), and the frames' digests (`digests.edn`, a run's): **pass**; its standard
  output `want.out`. Go text for now, not Go forms (WAS46).
- **`bin/wasm`**: `build DIR [MODULE]` writes the page (index.html, arbace.js, the module); `serve DIR [PORT]` a development server
  (`wasm/serve/main.go`, `net/http` on 127.0.0.1, `.wasm` as `application/wasm`) by hand only; `check` the shim's checks. Not run
  against the network by anything here.
- **`bin/wai js`** (`bin/lib/wai/js-run.bash`): the corpus `test/wasm/js-run.txt`, rows `js/...` against `test/wasm/js-results.edn`
  (**47 of 47**), classes `test/wasm/divergences-js.edn`, the controls; `--essential` (`test/wasm/essential-js.txt`, 43 rows). Builds by
  TamaGo's go for js/wasm, cached by key in the main checkout's `.tmp/wai-go/js`. Not a REAP cell (WAS17 (a)).

**WAS40 (a)**: `wai-go-aot` was already the fragment's `--full` step over `go-aot.txt`; the rest of the Go corpus at AOT is the new step
`wai-go-aot-rest` (`bin/wai go --aot --vs-eval`: each program at AOT judged against eval's record, program for program), with GT8.4's
`:rotate` over `test/wasm/go-results.edn` and the subset `test/wasm/subsets.edn` (go-aot.txt's 18), not spliced (WAS48 on its cost).
The fragment also gains `wai-js-essential` (essential) and `wai-js` (`--full`).

**The caveat from WA6** (`bin/wai go --aot` on the final generated bodies): see "Counts" and "Measured".

**Counts**:

| | before (WA6) | after (this branch) |
|---|---|---|
| syscall/js's tests on wai | no check (no js host) | **37 of 37 pass** |
| Go's own js programs | none | **6 of 6** |
| the demo's frames | none | **90 frames, 8 stated, all digests: pass** |
| the shim's checks | none | **imports 25 = 25 = 25 = 25; operation table 14 = 14** |
| js corpus (`bin/wai js`) | none | **47 of 47**; controls 7 fail, 2 checks pass |
| testsuite at eval (essential) | 18,601 commands | 18,601 (unchanged after HostCall) |
| the Go programs at AOT (`go-aot.txt`) | 18 of 18 | **18 of 18** (rerun on the final generated bodies, with HostCall: equal to the record, nothing re-recorded) |

**Measured** (one run at a time under the 2.5 GiB watcher, after night 13's gate): `bin/wai js` 23-24 s warm, 0.19 GB; its builds (the
test binary and seven programs) 7 s on a warm Go cache; `waidriver gojs` on syscall/js's tests 20 s and 0.18 GB, on the scene's 90
frames under a second; the conversions by repr/go's native reader: wai 105 s and 1.10 GB, the driver 115 s and 0.97 GB; `bin/wai
essential` 67 s. `bin/wai go --aot` over the 18 programs 2,694 s and 0.69 GB; `bin/wai aot-essential` 61 s and 0.55 GB.

**Proofs**: `bin/wai js` (47 of 47, controls) and `--essential` (43); `bin/wai essential` (18,601 commands, unchanged), `bin/wai go
--essential` (49 of 49), `bin/wai aot-essential` (16,234 commands, 1,779 of 1,779 functions), `bin/wai go --aot` (18 of 18), `bin/wai w2g
check` and `ops check`; the fragment spliced into a copy of `test/gate/steps.edn` (with its new `;;@ crops` and `;;@ controls` sections:
every step a control file) and `gate.crops check` on it: 0 problems; `bin/reap` 0 problems (53 of 56 cells, the WebAssembly row
unchanged: not a cell, WAS17 (a)); `bin/crops-score --record` (no score moved) and `--judge --strict` 0 problems; `make
check-makefile`; `test/gate/gate-key-crops-test.bash` passes and `bin/arbace-go --gate-key` is `c01784b6...` before and after; `bin/wasm
build` writes the page. Not run: `bin/wasm serve` (by hand only), the whole `wai-go-aot-rest` (about 57 hours), any browser.

**Proposals** (numbered after WAS42):

- **WAS43 eval in the headless world.** syscall/js's tests evaluate JavaScript text (an object literal with a function, an immediately
  invoked function with `Object.freeze`). (a) as built: a small subset of JavaScript evaluated over the world's values (doc/wasm/WAI.md
  §10.3), a SyntaxError naming the subset outside it; (b) a table of the strings the known programs pass; (c) a JavaScript crop first
  (WAS17 (b)). *Recommended: (a)*: (b) is no judge of a new program and (c) is a survey of its own; the subset grows with the programs.
- **WAS44 The world's clock.** (a) as built: virtual (the monotonic time 1 µs a reading and moved to the next timer or frame when the
  world waits; the wall clock from 2026-01-01), so a run is a function of its module, arguments and input; (b) the host's real clock, as
  Node's. *Recommended: (a)*: the frames and the timers' order are judged; a program measuring its own speed sees no real time.
- **WAS45 The headless world's file system.** (a) as built: the driver's mounts (host directories, a lexical sandbox, read-only ones
  EROFS), nothing outside them; (b) a memory tree, as the shim's. *Recommended: (a)* for the checks (Go's js programs read and write
  files as wasip1's do), the memory tree for the browser.
- **WAS46 The demo's form.** The brief asks for Go forms; repr/go's configurations are linux/amd64 and arm64 (`arbace.repr.go.load/config`
  refuses GOARCH=wasm), and a js/wasm configuration is a change to `arbace/repr/go`, inside gate_key. (a) as built: `wasm/demo/` as Go
  text (outside every key), converted to Go forms when repr/go reads js/wasm, at the key window W (with WA9's jrt and goi table files,
  WAS24 (a)); (b) the change to repr/go now, moving gate_key. *Recommended: (a)*.
- **WAS47 The development server's form.** (a) as built: `wasm/serve/main.go`, Go text built by TamaGo's go on demand, a person's tool;
  (b) Go forms with the demo at W. *Recommended: (a)* now, (b) with WAS46.
- **WAS48 The rotation's cost** (WAS40 (a)). GT8.4's seven slices give about 167 programs a night, about 3 minutes each when wai
  changed (8 hours: over the half share, WAS23), nothing on a night with wai unchanged (the AOT programs and the passes kept by key).
  (a) keep `wai-go-aot-rest` unspliced until WAS40 (c) (a Go program's runtime translated once, keyed by content) brings a program to
  seconds; (b) splice it now; (c) a slicing of its own (a 21st a night). *Recommended: (a)*.
- **WAS49 The frames' judge.** (a) as built: two levels, the authors' stated values (the arithmetic, within 1e-5) and a run's digests of
  every frame's calls (`--record`); (b) the digests alone. *Recommended: (a)*: the stated values are the judge independent of a run; the
  digests catch any other change of the stream.

**Not done**: Arbace's own js build and its REPL in the page (WA9: the key window), the scene from Arbace (`arbace.web.gpu`, WAS22's
second step), the oracle tier's headless Chromium pack and the Mac's checklist (no browser here), textures and meshes fetched as assets
(the shim's fs mounts no HTTP tree yet), the demo as Go forms (WAS46), WAS40 (c).

## As built: WAS40 (c), WA8 (the legacy exceptions) (2026-11-14, branch `wasm-wa8`)

Built from `mac/wasm-wa7` (`316d72073`); WAS40-WAS49 taken as recommended on that branch. doc/wasm/WAI.md §1, §3, §7, §9 and
doc/wasm/SPEC.md §3 describe the new parts. Everything is in `arbace_crop_paths` or outside every key: no key moves.

**WA8's state, checked first.** The plan's WA8 (§14) lists tail calls, exceptions, memory64, multiple memories, extended constants,
typed function references and GC, each at read, eval and AOT over the testsuite's root scripts. The testsuite's root is Wasm 3.0's:
those proposals' scripts are root scripts (`return_call*.wast`, `try_table.wast`, `throw*.wast`, `memory64.wast`, `memory_multi.wast`,
`*_ext_const`-style scripts, `call_ref.wast`, `br_on_*.wast`, `gc/`-style `struct.wast`, `array.wast`, `i31.wast`, `ref_cast.wast`,
...), and the records already held every root command passing at all three levels since WA4 and WA7 (eval 60,498 of 60,498, AOT the
same, read 7,182 of 7,182 cases): GC's type system (rec groups, subtyping, the casts) was built with wai in WA4 and with
arbace.wasm.valid in WA3. What WA8 had left was `legacy/`: the first exceptions proposal (`try`, `catch`, `catch_all`, `delegate`,
`rethrow`), 84 eval commands and 14 read cases classed `:legacy-exceptions`. This branch builds it at every level.

**WAS40 (c): w2g by shape** (WAS50 (a)). See doc/wasm/WAI.md §9.2. Go-built modules share their runtime and libraries but not their
constants (data addresses, function and global indices, the resume points' own function index), so WA7's translation a module shares
nothing and costs a build of about 37 packages a program. `W2GShape` writes a function with every immediate that is not a transfer of
control's read from its own code (`ins := (*[N]Ins)(code.Ins)`, no bounds check), so its Go is a function of its shape alone, and
`ShapeKey` (16 bytes of a SHA-256 over the counts, each instruction's op and height, the branches' arities, heights and targets,
br_table's entries, the handlers' clause kinds and targets) names it. `waidriver w2g --shared` writes each shape once for all the
modules given (`--verify` writes every function's Go and requires two of one key equal); `RegisterShapes` registers them; at
instantiation each function of a module the per-module registry does not name takes its shape's. A function over 2,000 instructions
is written as segments under a dispatcher (`w2gSegMax`: each segment a Go function entered at a pc, returning the next): Go's compiler
took over 2 GB and more than ten minutes (at a 1.5 GiB GOMEMLIMIT) on a 22,724-instruction runtime function in one piece, 19 s and 0.76 GB
in segments. `bin/wai go --aot` builds the programs and collects their modules (a first pass), translates a slice's modules by shape
into one AOT program (`WAI_AOT_SLICE`, default 400 programs: the disk's measure), runs the slice's programs on it (two at a time), and
removes the slice's build cache and program before the next.

| | before (WA7's translation a module) | after (by shape) |
|---|---|---|
| shapes | — | 50 programs: 66,905 functions, 1,983 shapes (78 MB of Go; `--verify`: no two functions of one shape differ); the whole build cache, 1,176 modules: 1,625,910 functions, 8,869 shapes, 364 MB of Go |
| `test/wasm/go-aot.txt`'s 18 programs | 2,694 s (about 150 s a program: 37 packages built each) | **390 s**: one program of 1,812 shapes, 90 MB, built in 260 s; 18 of 18 |
| the whole Go corpus at AOT | about 57 hours (estimated; never run) | **2,206 s for 1,188 programs** (three slices: 3,607, 5,080 and 4,283 shapes, programs of 187 to 239 MB built in 564 to 652 s; the runs about 6 minutes); peak 2.05 GB |
| the Go corpus's AOT record | 18 programs, 18 pass | **1,188 programs: 1,127 pass**, 61 :skip (eval's 59 build constraints and 2 actions); equal to eval's record program for program but the 3 over eval's budget, which pass at AOT |
| the testsuite at AOT (essential) | 16,234 of 16,234 | 16,234 of 16,234 (functions over 2,000 instructions now in segments in the per-module mode too) |

**WA8: the legacy exceptions** (WAS52 (a), WAS53 (a)).

- **Read** (`arbace/repr/wasm`): the forms `(try BT [INSTR...] [HANDLER...])`, HANDLER `(catch TAG [INSTR...])` or a last
  `(catch_all [INSTR...])`, `(try BT [INSTR...] (delegate L))` and `(rethrow L)` (doc/wasm/SPEC.md §3); the binary's 0x06, 0x07, 0x19,
  0x18 and 0x09 decoded and encoded (an instruction sequence also ends at a catch, a catch_all or a delegate); the text's plain and
  folded `try` (`(do ...)`, `(catch TAG ...)`, `(catch_all ...)`, `(delegate L)`, a delegate's label in the try's enclosing context),
  `rethrow`, and the misplaced keywords as `unexpected token`; the lexer's keywords beyond lexer.mll's at wg-3.0
  (`lex/legacy-keywords`: `try`, `do`, `catch`, `catch_all`, `delegate`, `rethrow`; the generated ops table is unchanged); the canonical
  printer's `try ... catch ... end` and `try ... delegate L`; arbace.wasm.valid's rules (a try's body as a block's, each catch's from
  its tag's values with its label marked a catch's, `rethrow` requiring one, `invalid rethrow label`; a delegate's label checked in
  the try's context, `unknown label`).
- **Eval** (wai; doc/wasm/WAI.md §3): lowered onto 3.0's handlers. A try's body is a handler's range closed at its first catch,
  catch_all or delegate; a catch is a clause (`catchLegacy`, `catchAllLegacy`) whose code starts after a jump to the try's end, the
  caught exception's reference in the slot at the try's height, below the tag's values, so `rethrow L` is `local.get` of label L's
  slot and `throw_ref`; a delegate is a clause (`catchDelegate`) counting the active handlers of the blocks between the try and its
  label, which `Machine.catch` skips. wai's validator takes the same rules (frames 0x06, 0x07, 0x19). w2g needs nothing more (a
  delegate clause is no target).
- **AOT**: the testsuite's AOT programs run the legacy scripts as the interpreter does (the same handlers, `X_catch`). Two fixes the
  AOT level found that eval had hidden: a catch block's results sit above its slot, and the interpreter's dynamic stack top carried
  them to the try's end, where w2g's static heights did not (three commands: the end of a catch is now a branch to the try's end,
  which moves them, as `br` does); and a function whose handlers no instruction can reach (no call or throw) has no `Lthrow`, so its
  catch targets are no labels (Go refuses an unused label).

**Counts**:

| | before (WA7's record) | after (this branch) |
|---|---|---|
| legacy/, read (`bin/repr-wasm wast`) | 14 cases classed `:legacy-exceptions`, 5 messages | **every case passes**: the testsuite 7,820 → 7,834 of 8,274 (the rest proposals beyond 3.0 and branch hints, classed), messages 4,998 → 5,012, print 2,760 → 2,765 |
| legacy/, eval (`bin/wai check`) | 5 pass, 84 classed `:legacy-exceptions` | **96 of 96 rows pass** (the commands 76 of 76); the testsuite 60,889 → 60,964 of 61,935; the validator's rows 5,578 → 5,592 agreeing; every other row unchanged |
| legacy/, AOT (`bin/wai check --aot`) | 76 classed | **78 of 78 rows pass**; the testsuite at AOT 60,964 of 61,935, equal to eval row for row (two messages of failing custom-descriptors rows name other canonical type ids: the batch's process) |
| the Go corpus at AOT | 18 programs | **1,188 programs, 1,127 pass**, 61 n/a (see above) |
| REAP, WebAssembly | read 7,840 / 0 / 454, eval 62,013 / 0 / 1,046, AOT 60,907 / 0 / 1,046, print 2,780 / 0 / 0 | **read 7,854 / 0 / 440, eval 62,088 / 0 / 971, AOT 62,091 / 0 / 971, print 2,785 / 0 / 0** |
| crops-score, WebAssembly | completeness 0.9779, maturity 0.9167, independence 1.0000 | **0.9790**, 0.9167, 1.0000 |

The class `:legacy-exceptions` is gone from `test/wasm/divergences-{wast,eval,aot}.edn` (no row left in it). The gate's fragment:
`wai-go-aot` is the whole corpus (`:mem-gib 2.2`), `wai-go-aot-rest` and `test/wasm/subsets.edn` removed (WAS51 (a)); still not
spliced.

**Measured** (one run at a time under the 2.5 GiB watcher; a morning gate rerun until 07:30 held the disk above 85%, so the first
runs were small dev steps only): `bin/wai go --aot` over the whole corpus 2,206 s, 2.05 GB at most (a slice's build); over go-aot.txt
390 s, 0.71 GB; `waidriver w2g --shared` over the 1,176 modules of the build cache 118 s, 1.49 GB (`--verify` over 50: 92 s, 0.38 GB);
`bin/repr-wasm wast` 347 s, 0.62 GB; `bin/wai check` 259-263 s, 0.70 GB; `bin/wai check --aot` 281-289 s, 0.68 GB; the conversions by
repr/go's native reader: wai 163 s at 0.95 GB (under a 750 MiB GOMEMLIMIT) and 1.18 GB without, the driver 0.85 GB.

**Proofs**: the fragment's essential commands (`bin/repr-wasm ops check && bin/repr-wasm wast --only test/wasm/essential-wast.txt`:
2,431 of 2,431, 1,427 messages, 1,004 prints; `bin/wai ops check && bin/wai essential`: 18,601 commands, 2,458 messages, 1,720
validator rows; `bin/wai go --essential` 49 of 49; `bin/wai w2g check && bin/wai aot-essential`: 16,234 commands; `bin/wai js
--essential` 43 of 43 with the frames); the whole `bin/repr-wasm wast`, `bin/wai check`, `bin/wai check --aot` and `bin/wai go --aot`
recorded, their controls failing as they must; `bin/wai divergences` and `--aot`; `bin/reap --record` then `bin/reap`: 0 problems (53
of 56 cells); `bin/crops-score --record` then `--judge --strict`: 0 problems; the fragment spliced into a copy of `test/gate/steps.edn`
and `gate.crops check` on it: 0 problems; `make check-makefile`; `test/gate/gate-key-crops-test.bash` passes and `bin/arbace-go
--gate-key` is `c01784b6...` before and after. The Go corpus's AOT record comes from the run before the last two fixes (the catch's
move and the unused labels): both change only functions with legacy handlers or with handlers and no raise, which no Go-built module
has (Go's wasm has no exceptions); on the final code `bin/wai go --aot --list test/wasm/go-aot.txt` passes 18 of 18 (335 s,
0.57 GB), judged on its programs' rows of the whole record. Not run: `bin/wai go` (eval's Go corpus: wai's interpreter changed only in its handler lookup and
the legacy instructions; its essential list passes), `bin/repr-wasm roundtrip` over the Go-built modules (the decoder's change is the
legacy opcodes and an instruction sequence's end at 0x07, 0x18, 0x19, none of which a Go module holds: about 18 minutes, 1.6 GB, a
night command), `bin/wai js` whole (its essential list passes).

**Not done**: the proposals beyond Wasm 3.0 (WAS4 (a); threads WA13), a persistent cache of shape packages across slices and runs (a
slice's build is most of the corpus's time: the same runtime shapes built three times), Arbace's own js build (WA9, the key window),
the scene from Arbace, the demo as Go forms (WAS46).

**Proposals** (numbered after WAS49; each taken as recommended on the branch):

- **WAS50 The translation shared across programs** (WAS40 (c)). (a) as built: by shape, every immediate that is not a transfer of
  control's read from the function's code, keyed by `ShapeKey`, one AOT program for a slice of programs; (b) by content with the
  constants kept (only identical functions shared: a Go program's runtime functions differ from another's in their data addresses and
  indices, so almost nothing would be); (c) a base of the runtime's functions built once and a delta a program through Go's build
  cache (two kinds of package, a base to choose and keep). *Recommended: (a)*: 1,625,910 functions are 8,869 shapes; the cost of
  reading the immediates from the code is the interpreter's own (the bodies are its cases), and the judge is unchanged.
- **WAS51 The Go corpus at AOT in the gate.** (a) as built: `wai-go-aot` runs the whole corpus (`bin/wai go --aot`: about 37 minutes,
  2.05 GB at most), its record the whole corpus, `go-aot.txt` a list for `--list`; WAS40 (a)'s rotation (`wai-go-aot-rest`,
  `test/wasm/subsets.edn`) removed, WAS48's question with it; (b) keep the rotation over the shared programs; (c) `go-aot.txt` only.
  *Recommended: (a)*: the whole corpus now costs less than a seventh of it did.
- **WAS52 The legacy exceptions in wai.** (a) as built: lowered onto Wasm 3.0's handlers (a try's body a handler's range, its catches
  clauses with the exception's reference in a slot of the catch block, `rethrow` a `local.get` of that slot and `throw_ref`, `delegate`
  a clause that skips the handlers between it and its label), so the interpreter, `Machine.catch` and w2g serve both; (b) an
  interpreter of their own for the legacy instructions (as the reference interpreter's `legacy` path). *Recommended: (a)*.
- **WAS53 The legacy exceptions' forms and keywords.** (a) as built: `try`, `delegate` and `rethrow` forms outside the generated
  instruction table (as `block` and `try_table` are), the lexer's keywords beyond wg-3.0's in `lex/legacy-keywords`; (b) regenerate
  ops.ae from the exception-handling proposal's legacy interpreter (a second pin for four scripts). *Recommended: (a)*.
- **WAS54 Large functions in w2g.** (a) as built: a function over 2,000 instructions written as segments under a dispatcher, in both
  modes; (b) a larger memory for Go's compiler (over 2.5 GiB for one runtime function, past the agents' limit); (c) large functions left
  to the interpreter (the AOT judge refuses them). *Recommended: (a)*.

## As built: queue 326, three proposals beyond Wasm 3.0 (2026-11-17, branch `326-wasm-deferred`)

The deferred classes triaged by what each proposal costs (the testsuite's pin fetched to the VM by its pin): **wide arithmetic**,
**the compact import section** and **custom page sizes** built at every level, their scripts recorded (the slices' rows alone, as
the judge's `--record` writes them) and their deferred classes gone; extended name section, branch hints, custom descriptors and
threads left deferred.

- **Wide arithmetic** (`proposals/wide-arithmetic`): `i64.add128`, `i64.sub128`, `i64.mul_wide_s`, `i64.mul_wide_u` (0xfc 19-22, no
  immediates). The instruction table takes them after decode.ml's rows (`test/wasm/gen_ops.ae`'s `proposal-rows`, their names among
  the keywords; `bin/repr-wasm ops check` still reproduces ops.ae byte for byte); arbace.wasm.valid's and wai's signatures, wai's
  stack effects and cases over `math/bits`, w2g's cases regenerated. Proved first on an own script of 114 assertions whose values
  Python's integers gave (eval and AOT 111 of 111, a corrupted copy failing on its two rows), then on the pin: read 10 of 10, eval
  and AOT 101 of 101.
- **The compact import section** (`proposals/compact-import-section`): binary, after an empty name 0x7f a vector of names and
  descriptions, 0x7e one description and a vector of names; text, `(import "m" (item "n" DESC)*)` and `(import "m" (item "n")*
  DESC)` (the second's description read once, one implicit type use even for no name, and no identifier: "identifier not
  allowed"). The forms are the imports the groups stand for; the layer's flags `[:imports]` keep the groups so a binary module
  prints back byte for byte (doc/wasm/SPEC.md §5). wai's decoder and validator read both encodings. Read 45 of 45, eval and AOT
  54 of 54.
- **Custom page sizes** (`proposals/custom-page-sizes`): `(pagesize N)` after a memory's limits (and before its inline data), the
  limits flag 0x08 with the log2 after them (forms `:page`, present when stated); valid page sizes 1 and 64 KiB, the bounds in
  pages of the size (2^32-1 for an i32 memory of 1-byte pages, none for an i64 one); wai's `Limits.CustomPage`/`PageExp` and
  `PageSize` in `memory.size`, `memory.grow`, instantiation and import matching (a page size must match exactly). Read 176 of 176,
  eval and AOT 74 of 74. Two messages differ by design: the i64 limit past 2^64 ('constant out of range', wg-3.0's lexer, where the
  proposal's interpreter says 'i64 constant out of range') and Wasm 3.0's binary.wast#82 (a memory's flag 0x08, malformed in 3.0,
  now a page size whose module ends: 'unexpected end of section or function'); both rejections stand.

wasm's cells: read 7,854 -> 7,934 pass (deferred 440 -> 360), eval 62,088 -> 62,288 and AOT 62,091 -> 62,291 (deferred 971 -> 771),
print 2,785 -> 2,838; completeness 0.9790 -> 0.9830.

**Not done**: the extended name section (the name section's subsections 3-11, the @name annotations of every kind and their
messages, the custom sections' own checks: 69 read and 12 eval rows), branch hints (3 read rows: the annotation's placement and
target checks), custom descriptors (descriptor types, exact references and their casts on top of GC: 215 read, 500 eval rows) and
threads (WA13, WAS26 (a): 73 read, 259 eval rows). With the first two built completeness would be about 0.986; 100 needs threads and
custom descriptors.

## As built: queue 326, round 2 (2026-11-17, branch `326-wasm-deferred`; the user's D326-1 (a), D326-2 (a))

Three more groups built and recorded, a commit each: **branch hints** (the annotations checked, a function's instruction's one:
'duplicate annotation', 'not in a function', the custom check's 'invalid target'; not kept in the forms), **the extended name
section** (`@name` at every binding's place with the proposal's messages; a `@custom "name"` decoded as the reference's name section,
subsections 0-13, `arbace.repr.wasm.binary/check-names`: malformed or a custom check; the names beyond the module's and the
functions' not kept) and **threads** (WAS26 (a)'s ordering lifted by the user: shared memories, the 67 atomics with a new kind `:zero`
for `atomic.fence`, wai on one thread: 'unaligned atomic', 'expected shared memory', wait 1 or 2, notify 0; w2g's cases regenerated).
The threads scripts' rules of Wasm 1.0 (one memory, one table, i32 limits) classed by design, `:threads-before-wasm-3.0`.

wasm's cells: read 7,934 -> 8,068 pass (by design 0 -> 11, deferred 360 -> 215), eval 62,288 -> 62,559 and AOT 62,291 -> 62,562
(deferred 771 -> 500), print 2,838 -> 2,871; completeness 0.9830 -> 0.9892.

**Not done**: custom descriptors (215 read, 500 eval rows): descriptor and describes clauses, exact references, their subtyping and
casts, six instructions, exact function imports; to derive from its 14 scripts (its Overview not pinned).

## As built: queue 327, custom descriptors (2026-11-17, branch `327-wasm-custom-descriptors`; the user's D326-4 (a))

Derived from the pinned testsuite's 14 scripts of `proposals/custom-descriptors/` (92a3d806; the proposal's Overview not pinned),
in three parts, a commit each: **exact references** (`(ref null? (exact X))`, 0x62; matched only by the same type or a bottom; an
allocation's and a defined or exactly imported function's reference exact; exact casts by the dynamic type itself; exact function
imports, 0x20), **descriptor types** (the describes and descriptor clauses, 0x4c and 0x4d, part of the types' identity, their rules
of placement, kind, finality and subtyping with the scripts' messages) and **the instructions** (`struct.new_desc`,
`struct.new_default_desc`, `ref.get_desc`, `ref.cast_desc_eq`, `br_on_cast_desc_eq` and `_fail`, 0xfb 0x20-0x26: 0x22 from the pin's
binary module, the rest in its order). The proposal's relaxation of `br_on_cast`'s rule conflicts with Wasm 3.0's own scripts: Wasm
3.0's kept, the proposal's two rows by design (the descriptor casts take the proposal's rule, which no Wasm 3.0 script contradicts).

wasm's cells: read 8,068 -> 8,281 pass (deferred 215 -> 0), eval 62,559 -> 63,057 and AOT 62,562 -> 63,060 (deferred 500 -> 0),
print 2,871 -> 2,935; completeness 0.9892 -> 0.9996 (meant 1.0000): every deferred row of the crop built.
