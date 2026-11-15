//go:build js && wasm

// Command scene is Arbace's WebGPU demo, its first step in Go (doc/wasm/WEB.md §5; WAS22 (a); Go text until repr/go reads js/wasm, WAS46): a lit rotating cube (one
// render pass a frame, a depth buffer) and a compute pass over a particle field, through the bridge's command buffer; the arrow
// keys change the spin; a fixed 60 Hz step. Its frames on the headless world are judged by test/wasm/js/scene/expected/frames.edn.
package main

import (
	"fmt"
	"math"
	"syscall/js"

	"arbace/webgpu"
)

const shader = `
struct U { proj: mat4x4<f32>, model: mat4x4<f32> };
@group(0) @binding(0) var<uniform> u: U;
struct Out { @builtin(position) pos: vec4<f32>, @location(0) color: vec3<f32> };
@vertex fn vs(@location(0) p: vec3<f32>, @location(1) c: vec3<f32>) -> Out {
  var o: Out;
  let w = u.model * vec4<f32>(p, 1.0);
  o.pos = u.proj * (w + vec4<f32>(0.0, 0.0, -4.0, 0.0));
  let light = 0.4 + 0.6 * max(dot(normalize(w.xyz), normalize(vec3<f32>(0.5, 0.8, 1.0))), 0.0);
  o.color = c * light;
  return o;
}
@fragment fn fs(i: Out) -> @location(0) vec4<f32> { return vec4<f32>(i.color, 1.0); }
`

const compute = `
struct P { pos: vec2<f32>, vel: vec2<f32> };
@group(0) @binding(0) var<storage, read_write> ps: array<P>;
@compute @workgroup_size(64) fn step(@builtin(global_invocation_id) id: vec3<u32>) {
  let i = id.x;
  if (i >= arrayLength(&ps)) { return; }
  var p = ps[i];
  p.pos = fract(p.pos + p.vel * (1.0 / 60.0) + 1.0);
  ps[i] = p;
}
`

// obj is a JavaScript object of the pairs in their order (js.ValueOf of a map would set them in Go's random order).
func obj(kv ...any) js.Value {
	o := js.Global().Get("Object").New()
	for i := 0; i+1 < len(kv); i += 2 {
		o.Set(kv[i].(string), kv[i+1])
	}
	return o
}

func arr(xs ...any) js.Value {
	a := js.Global().Get("Array").New()
	for _, x := range xs {
		a.Call("push", x)
	}
	return a
}

// perspective is WebGPU's (depth 0 to 1), column-major.
func perspective(fovy, aspect, near, far float64) []float32 {
	f := 1 / math.Tan(fovy/2)
	return []float32{float32(f / aspect), 0, 0, 0, 0, float32(f), 0, 0, 0, 0, float32(far / (near - far)), -1,
		0, 0, float32(near * far / (near - far)), 0}
}

// rotY is the rotation by a about the y axis, column-major.
func rotY(m []float32, a float64) {
	c, s := float32(math.Cos(a)), float32(math.Sin(a))
	copy(m, []float32{c, 0, -s, 0, 0, 1, 0, 0, s, 0, c, 0, 0, 0, 0, 1})
}

// the cube: 36 vertices, position and colour (each face its colour)
func cube() []float32 {
	faces := [][4][3]float32{
		{{-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}},
		{{1, -1, -1}, {-1, -1, -1}, {-1, 1, -1}, {1, 1, -1}},
		{{1, -1, 1}, {1, -1, -1}, {1, 1, -1}, {1, 1, 1}},
		{{-1, -1, -1}, {-1, -1, 1}, {-1, 1, 1}, {-1, 1, -1}},
		{{-1, 1, 1}, {1, 1, 1}, {1, 1, -1}, {-1, 1, -1}},
		{{-1, -1, -1}, {1, -1, -1}, {1, -1, 1}, {-1, -1, 1}},
	}
	colors := [][3]float32{{1, 0.3, 0.3}, {0.3, 1, 0.3}, {0.3, 0.3, 1}, {1, 1, 0.3}, {0.3, 1, 1}, {1, 0.3, 1}}
	var v []float32
	for i, f := range faces {
		for _, k := range []int{0, 1, 2, 0, 2, 3} {
			v = append(v, f[k][0], f[k][1], f[k][2], colors[i][0], colors[i][1], colors[i][2])
		}
	}
	return v
}

var (
	cmd   webgpu.Commands
	uni   = make([]float32, 32)
	verts = cube()
	parts = make([]float32, 4*256)
)

func main() {
	fmt.Println("scene: start")
	g := js.Global()
	canvas := g.Get("document").Call("getElementById", "screen")
	dpr := g.Get("devicePixelRatio").Float()
	w, h := int(canvas.Get("clientWidth").Float()*dpr), int(canvas.Get("clientHeight").Float()*dpr)
	canvas.Set("width", w)
	canvas.Set("height", h)
	gpu := g.Get("navigator").Get("gpu")
	if gpu.IsUndefined() {
		fmt.Println("scene: this browser has no WebGPU")
		return
	}
	adapter, err := webgpu.Await(gpu.Call("requestAdapter"))
	if err != nil || adapter.IsNull() {
		fmt.Println("scene: no adapter", err)
		return
	}
	device, err := webgpu.Await(adapter.Call("requestDevice"))
	if err != nil {
		fmt.Println("scene: no device", err)
		return
	}
	ctx := canvas.Call("getContext", "webgpu")
	format := gpu.Call("getPreferredCanvasFormat")
	ctx.Call("configure", obj("device", device, "format", format, "alphaMode", "opaque"))

	module := device.Call("createShaderModule", obj("code", shader))
	cmodule := device.Call("createShaderModule", obj("code", compute))
	vbuf := device.Call("createBuffer", obj("size", 4*len(verts), "usage", 0x20|0x8))
	ubuf := device.Call("createBuffer", obj("size", 4*len(uni), "usage", 0x40|0x8))
	pbuf := device.Call("createBuffer", obj("size", 4*len(parts), "usage", 0x80|0x8))
	depth := device.Call("createTexture", obj("size", arr(w, h), "format", "depth24plus", "usage", 0x10))
	pipe := device.Call("createRenderPipeline", obj(
		"layout", "auto",
		"vertex", obj("module", module, "entryPoint", "vs", "buffers", arr(obj("arrayStride", 24, "attributes", arr(
			obj("shaderLocation", 0, "offset", 0, "format", "float32x3"),
			obj("shaderLocation", 1, "offset", 12, "format", "float32x3"))))),
		"fragment", obj("module", module, "entryPoint", "fs", "targets", arr(obj("format", format))),
		"primitive", obj("topology", "triangle-list", "cullMode", "back"),
		"depthStencil", obj("format", "depth24plus", "depthWriteEnabled", true, "depthCompare", "less")))
	cpipe := device.Call("createComputePipeline", obj("layout", "auto", "compute", obj("module", cmodule, "entryPoint", "step")))
	bind := device.Call("createBindGroup", obj("layout", pipe.Call("getBindGroupLayout", 0),
		"entries", arr(obj("binding", 0, "resource", obj("buffer", ubuf)))))
	cbind := device.Call("createBindGroup", obj("layout", cpipe.Call("getBindGroupLayout", 0),
		"entries", arr(obj("binding", 0, "resource", obj("buffer", pbuf)))))

	dev, ctxH, depthH := webgpu.Register(device), webgpu.Register(ctx), webgpu.Register(depth)
	vbufH, ubufH, pbufH := webgpu.Register(vbuf), webgpu.Register(ubuf), webgpu.Register(pbuf)
	pipeH, cpipeH := webgpu.Register(pipe), webgpu.Register(cpipe)
	bindH, cbindH := webgpu.Register(bind), webgpu.Register(cbind)

	// the particles: a fixed field (positions on a grid, velocities by index)
	for i := 0; i < 256; i++ {
		parts[4*i], parts[4*i+1] = float32(i%16)/16, float32(i/16)/16
		parts[4*i+2], parts[4*i+3] = 0.05*float32(i%7-3), 0.05*float32(i%5-2)
	}
	copy(uni[:16], perspective(math.Pi/3, float64(w)/float64(h), 0.1, 100))
	rotY(uni[16:], 0)
	cmd.WriteBuffer(dev, vbufH, 0, webgpu.Bytes(verts))
	cmd.WriteBuffer(dev, ubufH, 0, webgpu.Bytes(uni))
	cmd.WriteBuffer(dev, pbufH, 0, webgpu.Bytes(parts))
	cmd.Exec()

	in := webgpu.NewInput(64)
	speed, steps, angle := 1, 0, 0.0
	var frame js.Func
	frame = js.FuncOf(func(this js.Value, a []js.Value) any {
		t := a[0].Float()
		in.Drain(func(e webgpu.Event) {
			if e.Type == webgpu.KeyDown {
				switch e.Code {
				case webgpu.ArrowRight:
					speed++
				case webgpu.ArrowLeft:
					speed--
				}
			}
		})
		// the fixed step: every 1/60 s due by t, each turning the cube by speed * 2π/120
		for due := int(math.Floor(t*60/1000 + 1e-9)); steps < due; steps++ {
			angle += float64(speed) * 2 * math.Pi / 120
		}
		rotY(uni[16:], angle)
		cmd.CreateCommandEncoder(dev)
		cmd.WriteBuffer(dev, ubufH, 64, webgpu.Bytes(uni[16:]))
		cmd.BeginComputePass()
		cmd.SetPipeline(cpipeH)
		cmd.SetBindGroup(0, cbindH)
		cmd.DispatchWorkgroups(4, 1, 1)
		cmd.End()
		cmd.BeginRenderPass(ctxH, depthH, true, 0.05, 0.05, 0.1, 1)
		cmd.SetPipeline(pipeH)
		cmd.SetBindGroup(0, bindH)
		cmd.SetVertexBuffer(0, vbufH, 0, 0)
		cmd.Draw(uint32(len(verts)/6), 1, 0, 0)
		cmd.End()
		cmd.Submit(dev)
		cmd.Exec()
		if steps%60 == 0 {
			fmt.Printf("scene: step %d angle %.4f\n", steps, angle)
		}
		g.Call("requestAnimationFrame", frame)
		return nil
	})
	g.Call("requestAnimationFrame", frame)
	fmt.Println("scene: running")
	select {}
}
