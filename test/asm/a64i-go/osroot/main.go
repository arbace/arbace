// a64i's Go program (queue 309, review 306's finding 2): os.Root over a directory with links os.Symlink made (symlinkat). Go's
// root_unix.go opens each name with O_NOFOLLOW and walks a link itself on ELOOP, so a link out of the root, absolute or relative,
// is "path escapes from parent" (a64i at 151aa8da7 read the file outside), one inside is followed; O_DIRECTORY's ENOTDIR (os.ReadDir
// of a file fails at its open), os.Link's hard link (linkat), Readlink and Lstat of a link.
package main

import (
	"fmt"
	"io"
	"os"
	"path/filepath"
)

func must(err error) {
	if err != nil {
		fmt.Println("setup:", err)
		os.Exit(1)
	}
}

func main() {
	d := "osroot.d"
	os.RemoveAll(d)
	r := filepath.Join(d, "root")
	must(os.MkdirAll(filepath.Join(r, "sub"), 0755))
	must(os.WriteFile(filepath.Join(d, "secret"), []byte("outside the root\n"), 0644))
	must(os.WriteFile(filepath.Join(r, "sub", "file"), []byte("inside the root\n"), 0644))
	abs, err := filepath.Abs(filepath.Join(d, "secret"))
	must(err)
	must(os.Symlink(abs, filepath.Join(r, "esc")))
	must(os.Symlink("../secret", filepath.Join(r, "up")))
	must(os.Symlink("sub/file", filepath.Join(r, "in")))
	must(os.Symlink("..", filepath.Join(r, "sub", "loop")))
	must(os.Link(filepath.Join(r, "sub", "file"), filepath.Join(r, "hard")))
	root, err := os.OpenRoot(r)
	must(err)
	for _, n := range []string{"esc", "up", "in", "hard", "sub/loop/in", "sub/loop/up", "sub/../up", "sub/file/x", "missing"} {
		f, err := root.Open(n)
		if err != nil {
			fmt.Printf("root.Open(%s): %v\n", n, err)
			continue
		}
		b, _ := io.ReadAll(f)
		f.Close()
		fmt.Printf("root.Open(%s): %q\n", n, b)
	}
	if _, err := root.OpenFile("esc", os.O_WRONLY|os.O_TRUNC, 0); err != nil {
		fmt.Println("root.OpenFile(esc, O_TRUNC):", err)
	}
	if t, err := root.Readlink("up"); err == nil {
		fmt.Println("root.Readlink(up):", t)
	} else {
		fmt.Println("root.Readlink(up):", err)
	}
	if fi, err := root.Lstat("esc"); err == nil {
		fmt.Println("root.Lstat(esc):", fi.Mode().Type() == os.ModeSymlink)
	}
	if fi, err := root.Stat("in"); err == nil {
		fmt.Println("root.Stat(in):", fi.Mode().IsRegular(), fi.Size())
	}
	if _, err := root.Stat("esc"); err != nil {
		fmt.Println("root.Stat(esc):", err)
	}
	sub, err := root.OpenRoot("sub")
	must(err)
	if _, err := sub.Open("loop/in"); err != nil {
		fmt.Println("sub.Open(loop/in):", err)
	}
	a, _ := os.Stat(filepath.Join(r, "sub", "file"))
	b, _ := os.Stat(filepath.Join(r, "hard"))
	fmt.Println("os.Link: same file:", os.SameFile(a, b))
	b2, _ := os.ReadFile(filepath.Join(d, "secret"))
	fmt.Printf("secret: %q\n", b2)
	_, err = os.ReadDir(filepath.Join(d, "secret"))
	fmt.Println("os.ReadDir(file):", err)
	sub.Close()
	root.Close()
	must(os.RemoveAll(d))
}
