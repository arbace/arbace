//go:build upstream

package main

import (
	"bufio"
	"fmt"
)

// The types mode is go-whim's cc's alone (types.go): the upstream build has none.
func dumpTypes(w *bufio.Writer, e entry, h *host) {
	fmt.Fprintf(w, "#error\tthe types mode needs go-whim's cc\n#notree\n")
}
