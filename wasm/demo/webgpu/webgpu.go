//go:build js && wasm

// Package webgpu is the Go side of Arbace's WebGPU bridge (doc/wasm/WEB.md; WAS16 (a)): setup through syscall/js (the shim's
// handle table holding the GPU objects, Go holding their handles), and each frame one command buffer written into linear memory,
// its operations named after webgpu.h's functions, run by the shim's arbace.gpu.exec in one call. The input ring (doc/wasm/WEB.md
// §3) is read the same way: the shim writes the page's events into linear memory, Go drains them once a frame.
package webgpu

import (
	"errors"
	"syscall/js"
	"unsafe"
)

// Handle is a GPU object in the shim's handle table (0: none).
type Handle uint32

var bridge = js.Global().Get("arbace")

// Register puts a JavaScript object in the handle table.
func Register(v js.Value) Handle { return Handle(bridge.Get("gpu").Call("handle", v).Int()) }

// Release frees a handle.
func Release(h Handle) { bridge.Get("gpu").Call("release", int(h)) }

// Await waits for a promise: its value, or its rejection as an error. The goroutine parks; Go returns to the event loop.
func Await(p js.Value) (js.Value, error) {
	ok, bad := make(chan js.Value, 1), make(chan js.Value, 1)
	fok := js.FuncOf(func(this js.Value, a []js.Value) any { ok <- a[0]; return nil })
	fbad := js.FuncOf(func(this js.Value, a []js.Value) any { bad <- a[0]; return nil })
	defer fok.Release()
	defer fbad.Release()
	p.Call("then", fok, fbad)
	select {
	case v := <-ok:
		return v, nil
	case e := <-bad:
		return js.Undefined(), errors.New(e.Call("toString").String())
	}
}

// The operations (doc/wasm/WEB.md §2), the shim's and the headless world's table.
const (
	opDeviceCreateCommandEncoder     = 1
	opCommandEncoderBeginRenderPass  = 2
	opPassSetPipeline                = 3
	opPassSetBindGroup               = 4
	opRenderPassSetVertexBuffer      = 5
	opRenderPassSetIndexBuffer       = 6
	opRenderPassDraw                 = 7
	opRenderPassDrawIndexed          = 8
	opPassEnd                        = 9
	opCommandEncoderBeginComputePass = 10
	opComputePassDispatchWorkgroups  = 11
	opQueueWriteBuffer               = 12
	opQueueSubmit                    = 13
)

// Commands is a frame's command buffer: u32 words, each operation its number then its arguments.
type Commands struct {
	w []uint32
}

func (c *Commands) op(args ...uint32) { c.w = append(c.w, args...) }

func f32(f float32) uint32 { return *(*uint32)(unsafe.Pointer(&f)) }

// Addr is a byte slice's address in linear memory (it must stay alive until Exec).
func Addr(b []byte) uint32 {
	if len(b) == 0 {
		return 0
	}
	return uint32(uintptr(unsafe.Pointer(&b[0])))
}

// Bytes is a float32 slice's bytes (the same memory).
func Bytes(f []float32) []byte {
	if len(f) == 0 {
		return nil
	}
	return unsafe.Slice((*byte)(unsafe.Pointer(&f[0])), 4*len(f))
}

func (c *Commands) CreateCommandEncoder(device Handle) {
	c.op(opDeviceCreateCommandEncoder, uint32(device))
}

// BeginRenderPass begins a pass on the canvas context's current texture, with a depth texture (0 none), cleared or loaded.
func (c *Commands) BeginRenderPass(context, depth Handle, clear bool, r, g, b, a float32) {
	load := uint32(2)
	if clear {
		load = 1
	}
	c.op(opCommandEncoderBeginRenderPass, uint32(context), uint32(depth), load, f32(r), f32(g), f32(b), f32(a))
}

func (c *Commands) SetPipeline(p Handle)                { c.op(opPassSetPipeline, uint32(p)) }
func (c *Commands) SetBindGroup(index uint32, g Handle) { c.op(opPassSetBindGroup, index, uint32(g)) }
func (c *Commands) SetVertexBuffer(slot uint32, b Handle, offset, size uint32) {
	c.op(opRenderPassSetVertexBuffer, slot, uint32(b), offset, size)
}

// SetIndexBuffer: format 1 uint16, 2 uint32.
func (c *Commands) SetIndexBuffer(b Handle, format, offset, size uint32) {
	c.op(opRenderPassSetIndexBuffer, uint32(b), format, offset, size)
}
func (c *Commands) Draw(vertices, instances, firstVertex, firstInstance uint32) {
	c.op(opRenderPassDraw, vertices, instances, firstVertex, firstInstance)
}
func (c *Commands) DrawIndexed(indices, instances, firstIndex uint32, baseVertex int32, firstInstance uint32) {
	c.op(opRenderPassDrawIndexed, indices, instances, firstIndex, uint32(baseVertex), firstInstance)
}
func (c *Commands) End()              { c.op(opPassEnd) }
func (c *Commands) BeginComputePass() { c.op(opCommandEncoderBeginComputePass) }
func (c *Commands) DispatchWorkgroups(x, y, z uint32) {
	c.op(opComputePassDispatchWorkgroups, x, y, z)
}

// WriteBuffer writes data (a view of linear memory, no copy through Go) at offset of a buffer.
func (c *Commands) WriteBuffer(device, buffer Handle, offset uint32, data []byte) {
	c.op(opQueueWriteBuffer, uint32(device), uint32(buffer), offset, Addr(data), uint32(len(data)))
}

// Submit finishes the encoder and submits it.
func (c *Commands) Submit(device Handle) { c.op(opQueueSubmit, uint32(device)) }

// Exec runs the buffer in the shim (one call) and empties it.
func (c *Commands) Exec() {
	if len(c.w) == 0 {
		return
	}
	b := unsafe.Slice((*byte)(unsafe.Pointer(&c.w[0])), 4*len(c.w))
	bridge.Get("gpu").Call("exec", Addr(b), len(b))
	c.w = c.w[:0]
}

// Event is an input event of the ring: Type 1 keydown, 2 keyup, 3 pointermove, 4 pointerdown, 5 pointerup, 6 wheel, 7 resize;
// Code the key's number (doc/wasm/WEB.md §3: KeyA..KeyZ 1-26, Digit0..9 27-36, ArrowUp 37, ArrowDown 38, ArrowLeft 39,
// ArrowRight 40, Space 41, Enter 42, Escape 43, ShiftLeft 44, ControlLeft 45, Backspace 46, Tab 47).
type Event struct {
	Type, Code uint32
	X, Y       float32
}

const (
	KeyDown     = 1
	KeyUp       = 2
	PointerMove = 3
	PointerDown = 4
	PointerUp   = 5
	Wheel       = 6
	Resize      = 7

	ArrowUp    = 37
	ArrowDown  = 38
	ArrowLeft  = 39
	ArrowRight = 40
	Space      = 41
)

// Input is the ring the shim writes events into: the head (the shim's count), the tail (Go's), then the records.
type Input struct {
	ring []uint32
	cap  uint32
}

// NewInput registers a ring of n events with the shim.
func NewInput(n int) *Input {
	in := &Input{ring: make([]uint32, 2+4*n), cap: uint32(n)}
	b := unsafe.Slice((*byte)(unsafe.Pointer(&in.ring[0])), 4*len(in.ring))
	bridge.Get("input").Call("ring", Addr(b), n)
	return in
}

// Drain hands each event written since the last drain to fn.
func (in *Input) Drain(fn func(Event)) {
	head, tail := in.ring[0], in.ring[1]
	for tail != head {
		r := in.ring[2+4*(tail%in.cap):]
		x, y := r[2], r[3]
		fn(Event{Type: r[0], Code: r[1], X: *(*float32)(unsafe.Pointer(&x)), Y: *(*float32)(unsafe.Pointer(&y))})
		tail++
	}
	in.ring[1] = tail
}
