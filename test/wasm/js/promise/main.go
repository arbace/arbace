//go:build js && wasm

// promise: JavaScript promises awaited by Go (then's callbacks as js.Func), resolved by the world's microtasks; a rejection.
package main

import (
	"fmt"
	"syscall/js"
)

func await(p js.Value) (js.Value, bool) {
	ok, bad := make(chan js.Value, 1), make(chan js.Value, 1)
	f := js.FuncOf(func(this js.Value, a []js.Value) any { ok <- a[0]; return nil })
	g := js.FuncOf(func(this js.Value, a []js.Value) any { bad <- a[0]; return nil })
	defer f.Release()
	defer g.Release()
	p.Call("then", f, g)
	select {
	case v := <-ok:
		return v, true
	case e := <-bad:
		return e, false
	}
}

func main() {
	v, ok := await(js.Global().Get("Promise").Call("resolve", 41).Call("then", js.Global().Call("eval", "(x) => x + 1")))
	fmt.Println("resolved", v.Int(), ok)
	p := js.Global().Call("eval", "new Promise(function (res, rej) { setTimeout(function () { rej(new Error('late')); }, 5); })")
	e, ok := await(p)
	fmt.Println("rejected", e.Get("message").String(), ok)
	chained := js.Global().Call("eval", "Promise.resolve(2).then(function (x) { return Promise.resolve(x * 10); }).then(function (y) { return y + 1; })")
	v, _ = await(chained)
	fmt.Println("chained", v.Int())
}
