# Arbace in the browser: the shim, the bridge and the demo

The browser side of the WebAssembly crop (doc/wasm/WASM-SURVEY.md §9, WAS15 (a), WAS16 (a), WAS17 (a), WAS22 (a); "As built: WA11,
WA12 (begun)"). The user's focus: a minimal HTML and JavaScript shim, Wasm in the browser as the single-page WebGPU games do it. Go's
`js/wasm` port is the guest (Arbace's own build for `js` waits for WA9); wai's headless world (doc/wasm/WAI.md §10) is the host the
checks run it on. No browser and no JavaScript engine runs at check time; a real browser is the oracle tier's, by hand.

## 1. The page (`wasm/web/`)

Two files and the module, no npm, no build step:

- **`index.html`**: one `<canvas id="screen">` filling the page, a `<pre id="console">`, an input line `#line` (the REPL's standard
  input, WAS13 (a)), one `<script type="module" src="arbace.js">`.
- **`arbace.js`**, in six parts:
  1. **the gojs imports and the `Go` class**, derived from the pinned TamaGo go1.27.1's `lib/wasm/wasm_exec.js` (BSD-3, LICENSE.md's Go
     section): the 24 imports and wasm_exec.js's `debug` kept as they are, `run`, `_resume`, `_makeFuncWrapper`; only the placeholders
     wasm_exec.js puts in `globalThis.fs` and `process` replaced by part 2, and the exit's message sent to the console;
  2. **the file system**: Node's callback API as `syscall/fs_js.go` calls it (`open`, `read`, `write`, `fstat`, `stat`, `readdir`,
     `mkdir`, `unlink`, `rename`, ...; Linux's flag values in `fs.constants`) over a tree held in memory (`/` and `/tmp`), standard
     output and error line-buffered into the console, standard input fed by the input line (a read waits for the next line);
     `process` (`cwd`, `chdir`, the ids -1) and `path.resolve`;
  3. **the frame loop**: the browser's own `requestAnimationFrame`, which the Go side calls; a `ResizeObserver` keeping the canvas at
     its box times `devicePixelRatio` (a `resize` event into the ring);
  4. **the input ring** (§3);
  5. **the WebGPU bridge** (§2);
  6. **the start**: `WebAssembly.instantiateStreaming(fetch(URL))` (`?wasm=URL`, default `arbace.wasm`; `&arg=` the program's
     arguments), `go.run`, errors to the console; a browser without `navigator.gpu` gets a line, not a fallback renderer.
- **The checks** (`bin/wai shim-check`, in `bin/wai js`'s rows `js/shim/*`): the names of `arbace.js`'s gojs object equal the pinned
  Go's `//go:wasmimport gojs` directives (24, with `debug`), `wasm_exec.js`'s and wai's headless world's (`waidriver gojs-imports`), so
  a Go update cannot leave the shim behind silently; the bridge's operation table (`nargs`) equals the world's (`waidriver gpu-ops`).
  Their negative controls: a copy of `arbace.js` without one import, and one with an argument count changed, must fail.
- **`bin/wasm`**: `build DIR [MODULE]` writes the page (the two files and the module as `arbace.wasm`; by default the demo scene);
  `serve DIR [PORT]` serves it on `http://127.0.0.1:PORT/` (`wasm/serve`, `net/http.FileServer` with `.wasm` as `application/wasm`,
  built by TamaGo's go): a tool for a person, never run by a check (WebGPU needs a secure context: `localhost` is one).

## 2. The WebGPU bridge (WAS16 (a))

**Setup through `syscall/js`** (rare calls): `navigator.gpu.requestAdapter()`, `requestDevice()`, `canvas.getContext("webgpu")`,
`configure`, shader modules from WGSL text, buffers, textures, pipelines, bind groups; each promise awaited by a channel
(`webgpu.Await`). The objects stay on the JavaScript side in the shim's **handle table** (`arbace.gpu.handle(obj)` → a small integer,
`release(h)`); Go holds the handles.

**The frame as one command buffer** in linear memory: u32 words, little-endian, each operation its number then its arguments, run by
`arbace.gpu.exec(pointer, length)` in one call (no copy through `syscall/js`). The operations, named after `webgpu.h`'s functions:

| op | name | arguments (u32 unless said) | the shim's call |
|---:|---|---|---|
| 1 | `DeviceCreateCommandEncoder` | device | `encoder = device.createCommandEncoder()` |
| 2 | `CommandEncoderBeginRenderPass` | context, depth texture (0 none), load (1 clear, 2 load), r, g, b, a (f32) | `pass = encoder.beginRenderPass({colorAttachments: [{view: context.getCurrentTexture().createView(), loadOp, storeOp: "store", clearValue}], depthStencilAttachment?})` |
| 3 | `PassSetPipeline` | pipeline | `pass.setPipeline` (a render or a compute pass) |
| 4 | `PassSetBindGroup` | index, bind group | `pass.setBindGroup` |
| 5 | `RenderPassSetVertexBuffer` | slot, buffer, offset, size (0 the rest) | `pass.setVertexBuffer` |
| 6 | `RenderPassSetIndexBuffer` | buffer, format (1 `uint16`, 2 `uint32`), offset, size (0 the rest) | `pass.setIndexBuffer` |
| 7 | `RenderPassDraw` | vertices, instances, first vertex, first instance | `pass.draw` |
| 8 | `RenderPassDrawIndexed` | indices, instances, first index, base vertex (i32), first instance | `pass.drawIndexed` |
| 9 | `PassEnd` | | `pass.end()` |
| 10 | `CommandEncoderBeginComputePass` | | `pass = encoder.beginComputePass()` |
| 11 | `ComputePassDispatchWorkgroups` | x, y, z | `pass.dispatchWorkgroups` |
| 12 | `QueueWriteBuffer` | device, buffer, offset, pointer, length | `device.queue.writeBuffer(buffer, offset, new Uint8Array(memory, pointer, length))`: a view of linear memory |
| 13 | `QueueSubmit` | device | `device.queue.submit([encoder.finish()])` |

The Go side is the package `arbace/webgpu` (`wasm/demo/webgpu/webgpu.go`; Go text until repr/go reads js/wasm, WAS46): `Register`, `Release`, `Await`,
`Commands` (an encoder method per operation, `Exec`), `Bytes`/`Addr` (a slice's linear-memory view and address). The headless world's
decoder (`go/arbace/wai/gojspage.ae`'s `gpuBridge.exec`) is the shim's statement for statement over its recording fakes, so the
per-frame stream of WebGPU calls is data and judged (§5). Breadth later (WAS16 (c)): the Go API generated from `webgpu.idl`.

## 3. The input ring

One callback a frame, not one an event (§9.2's item 4): Go registers a ring in its memory (`arbace.input.ring(pointer, capacity)`,
`webgpu.NewInput`); the shim writes each event at the head, Go drains from the tail at the frame's start (`Input.Drain`). Layout: u32
head (the shim's count), u32 tail (Go's), then the records of 16 bytes: u32 type (1 keydown, 2 keyup, 3 pointermove, 4 pointerdown, 5
pointerup, 6 wheel, 7 resize), u32 code (a key: `KeyA`..`KeyZ` 1-26, `Digit0`..`Digit9` 27-36, `ArrowUp` 37, `ArrowDown` 38,
`ArrowLeft` 39, `ArrowRight` 40, `Space` 41, `Enter` 42, `Escape` 43, `ShiftLeft` 44, `ControlLeft` 45, `Backspace` 46, `Tab` 47, else
0; a pointer event: the button), f32 x, f32 y (the pointer's position in canvas pixels; the wheel's deltas; resize's width and
height). A full ring drops the event.

## 4. The headless world's page

doc/wasm/WAI.md §10.5: `document` with the one canvas, `window` the global, `navigator.gpu` and every GPU object a **recording fake**
(each call a line: `GPUDevice#1.createBuffer({size: 128, usage: 72}) -> GPUBuffer#2`; a WGSL text or a long string by its length and
digest; bytes by their length, digest and first 16 float32s), `requestAnimationFrame` stepped by the world (frame k at k × 1000/60
ms of the virtual clock), input events injected from a script (`waidriver gojs --input FILE`: `FRAME TYPE [CODE [X Y]]` a line,
delivered before the frame's callbacks, into the ring and to the `addEventListener` listeners), and the recording written by frame
(`--record FRAMES.EDN`).

## 5. The demo (WAS22 (a): Go first)

**The scene** (`wasm/demo/scene/main.go`, package main of the module `arbace` in `wasm/demo/`, the bridge's package beside it;
Go text, not Go forms yet: repr/go's configurations are linux/amd64 and arm64, and a js/wasm one is a change to `arbace/repr/go`,
inside the keys: WAS46): a lit rotating cube (36
vertices, a colour a face, a depth buffer, one render pass a frame) and a compute pass over a field of 256 particles; the arrow keys
change the spin; a fixed 60 Hz step (each step turns the cube by speed × 2π/120). Built for js/wasm by TamaGo's go (`bin/wai scene-build`; `bin/wasm build DIR` writes the page with it).

**Its judge** (`bin/wai js`'s row `js/scene`, `test/wasm/frames.ae`): 90 frames on the headless world with the input script
`test/wasm/js/scene/input.txt` (ArrowRight before frame 31); standard output against `want.out`; the recording against
`expected/frames.edn`, the authors' statements written from the scene's arithmetic (the setup's projection matrix; the model matrix
at frames 1, 15, 30, 31, 45, 60 and 90, each float within 1e-5), and each frame's digest of its lines against `expected/digests.edn`
(a run's, `bin/wai js --record`). Its controls: the recording judged one frame off (`shift 1`) and a run without the key press
(`input-none.txt`) must fail. That judges the Go side and the bridge's protocol; it does not judge JavaScript, WebGPU or a GPU (§10.3
of the survey): the shim's code is small and reviewed, its import list and operation table checked.

**Next** (WA12's rest): the same scene from Arbace (`arbace.web.gpu`, a namespace over the bridge, with the REPL in the page: a form
typed at the input line changes the next frame), once Arbace's own js build exists (WA9); the oracle tier's headless Chromium with
WebGPU on SwiftShader (frames read back, a pack with a tolerance) and the Mac's checklist (Safari and Chrome on Metal), by hand.
