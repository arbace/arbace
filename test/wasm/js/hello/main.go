//go:build js && wasm

// hello: standard output and error, console.log, the arguments and the environment on the headless world.
package main

import (
	"fmt"
	"os"
	"syscall/js"
)

func main() {
	fmt.Println("hello, js/wasm")
	fmt.Println("args", len(os.Args), os.Args[1:])
	fmt.Println("HELLO", os.Getenv("HELLO"))
	js.Global().Get("console").Call("log", "console.log:", 42, true, js.Null())
	fmt.Println("typeof window", js.Global().Get("window").Type(), "document", js.Global().Get("document").Type())
	fmt.Println("2 ** 10 by eval:", js.Global().Call("eval", "(function (n) { var r = 1; for (var i = 0; i < n; i++) { r = r * 2; } return r; })(10)").Int())
}
