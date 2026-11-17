//go:build js && wasm

// callback: JavaScript calling Go synchronously (a js.Func called from code the world evaluates, inside Go's own call), nested
// twice; a JavaScript exception turned into Go's panic of js.Error and recovered.
package main

import (
	"fmt"
	"syscall/js"
)

func main() {
	sq := js.FuncOf(func(this js.Value, a []js.Value) any { return a[0].Int() * a[0].Int() })
	defer sq.Release()
	sum := js.Global().Call("eval", "(function (f, n) { var s = 0; for (var i = 1; i <= n; i++) { s += f(i); } return s; })")
	fmt.Println("sum of squares 1..10:", sum.Invoke(sq, 10).Int())
	var outer js.Func
	outer = js.FuncOf(func(this js.Value, a []js.Value) any {
		return sum.Invoke(sq, a[0]).Int() + 1
	})
	defer outer.Release()
	fmt.Println("nested:", js.Global().Call("eval", "(function (g) { return g(3) * 2; })").Invoke(outer).Int())
	func() {
		defer func() {
			r := recover()
			if e, ok := r.(js.Error); ok {
				fmt.Println("recovered:", e.Error())
			} else {
				fmt.Println("recovered something else:", r)
			}
		}()
		js.Global().Call("eval", "(function () { throw new TypeError('boom'); })").Invoke()
	}()
}
