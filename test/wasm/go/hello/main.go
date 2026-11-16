package main

import (
	"fmt"
	"time"
)

func main() {
	ch := make(chan int)
	go func() { ch <- 42 }()
	fmt.Println("hello from wasm", <-ch, time.Now().Year() > 2000)
}
