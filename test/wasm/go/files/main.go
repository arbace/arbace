// Files on WASI's preopened directories: create, write, append, seek, read back, stat, rename, readdir, a symbolic link, remove,
// and the sandbox's refusals (a path above the preopen, a write under the read-only one) (WA5's own programs; the check preopens
// /tmp as a scratch directory and the working directory read-only).
package main

import (
	"fmt"
	"io"
	"os"
	"path/filepath"
	"sort"
)

func check(what string, err error) {
	if err != nil {
		fmt.Println(what, "error:", err)
		os.Exit(1)
	}
}

func main() {
	dir := filepath.Join(os.TempDir(), "wai-files")
	check("mkdir", os.MkdirAll(dir, 0o755))
	p := filepath.Join(dir, "a.txt")
	check("write", os.WriteFile(p, []byte("hello\n"), 0o644))
	f, err := os.OpenFile(p, os.O_APPEND|os.O_WRONLY, 0)
	check("open append", err)
	_, err = f.WriteString("world\n")
	check("append", err)
	check("close", f.Close())
	f, err = os.Open(p)
	check("open", err)
	_, err = f.Seek(6, io.SeekStart)
	check("seek", err)
	rest, err := io.ReadAll(f)
	check("read", err)
	f.Close()
	fmt.Printf("after seek: %q\n", rest)
	st, err := os.Stat(p)
	check("stat", err)
	fmt.Println("size", st.Size(), "regular", st.Mode().IsRegular())
	q := filepath.Join(dir, "b.txt")
	check("rename", os.Rename(p, q))
	check("symlink", os.Symlink("b.txt", filepath.Join(dir, "link")))
	b, err := os.ReadFile(filepath.Join(dir, "link"))
	check("read link", err)
	fmt.Printf("through the link: %q\n", b)
	check("mkdir sub", os.Mkdir(filepath.Join(dir, "sub"), 0o755))
	es, err := os.ReadDir(dir)
	check("readdir", err)
	var names []string
	for _, e := range es {
		names = append(names, e.Name())
	}
	sort.Strings(names)
	fmt.Println("entries", names)
	_, err = os.Stat(filepath.Join(dir, "a.txt"))
	fmt.Println("old name gone:", os.IsNotExist(err))
	check("remove all", os.RemoveAll(dir))
	_, err = os.Stat(dir)
	fmt.Println("removed:", os.IsNotExist(err))
	wd, _ := os.Getwd()
	_, err = os.ReadFile(filepath.Join(wd, "../../../../../../../../etc/passwd"))
	fmt.Println("above the preopen refused:", err != nil)
	err = os.WriteFile(filepath.Join(wd, "wai-should-not-exist.txt"), []byte("x"), 0o644)
	fmt.Println("write under the read-only preopen refused:", err != nil)
}
