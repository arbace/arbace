//go:build js && wasm

// deadlock: a program whose goroutines all block with nothing pending: the world resumes it with event 0 as
// wasm_exec_node.js's exit handler does, and Go reports its deadlock (status 2).
package main

import "fmt"

func main() {
	fmt.Println("waiting")
	<-make(chan int)
}
