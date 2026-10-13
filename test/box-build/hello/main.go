// Command hello is the box's build's test program (doc/go/BOX-NOTES.md, S7): a
// guest on the board, which arbace.box.build compiles and links in the box;
// the launcher then boots it from the store.
package main

import (
	"os"

	"arbace/box/board"
)

func main() {
	args := board.Args()
	msg := "hello from a guest compiled and linked in the box"
	if len(args) > 1 {
		msg += ": " + args[1]
	}
	println(msg)
	os.Exit(len(args) - 1)
}
