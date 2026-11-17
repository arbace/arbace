//go:build js && wasm

// files: Go's os package over the world's fs (Node's callback API): a directory mounted at /tmp, files written, read, listed,
// renamed and removed; an absent file's error; outside every mount nothing exists.
package main

import (
	"errors"
	"fmt"
	"io/fs"
	"os"
	"sort"
)

func main() {
	if err := os.MkdirAll("/tmp/d/e", 0o755); err != nil {
		fmt.Println("mkdir:", err)
		return
	}
	if err := os.WriteFile("/tmp/d/a.txt", []byte("alpha\n"), 0o644); err != nil {
		fmt.Println("write:", err)
		return
	}
	f, _ := os.OpenFile("/tmp/d/a.txt", os.O_APPEND|os.O_WRONLY, 0)
	f.WriteString("beta\n")
	f.Close()
	b, err := os.ReadFile("/tmp/d/a.txt")
	fmt.Printf("read %q %v\n", b, err)
	st, _ := os.Stat("/tmp/d/a.txt")
	fmt.Println("size", st.Size(), "dir", st.IsDir(), "mode", st.Mode().Perm())
	os.Rename("/tmp/d/a.txt", "/tmp/d/b.txt")
	es, _ := os.ReadDir("/tmp/d")
	var names []string
	for _, e := range es {
		names = append(names, fmt.Sprint(e.Name(), " ", e.IsDir()))
	}
	sort.Strings(names)
	fmt.Println("entries", names)
	_, err = os.ReadFile("/tmp/d/a.txt")
	fmt.Println("absent:", errors.Is(err, fs.ErrNotExist))
	_, err = os.Stat("/etc/passwd")
	fmt.Println("outside the mounts:", errors.Is(err, fs.ErrNotExist))
	fmt.Println("remove:", os.Remove("/tmp/d/b.txt"), os.Remove("/tmp/d/e"), os.Remove("/tmp/d"))
	wd, err := os.Getwd()
	fmt.Println("cwd", wd, err)
}
