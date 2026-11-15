// a64i's Go program (queue 309, review 306's finding 2): os.Root over a directory with links os.Symlink made (symlinkat). Go's
// root_unix.go opens each name with O_NOFOLLOW and walks a link itself on ELOOP, so a link out of the root, absolute or relative,
// is "path escapes from parent" (a64i at 151aa8da7 read the file outside), one inside is followed; O_DIRECTORY's ENOTDIR (os.ReadDir
// of a file fails at its open), os.Link's hard link (linkat), Readlink and Lstat of a link. Queue 314: every os.Root method (Name,
// Create, WriteFile, ReadFile, Mkdir and MkdirAll: mkdirat at the root's descriptor, Chmod: fchmodat2 with AT_SYMLINK_NOFOLLOW, Chtimes:
// utimensat, Chown and Lchown: fchownat, Symlink, Readlink, Link, Rename, OpenFile, Remove, RemoveAll, Stat, Lstat, OpenRoot, FS),
// each inside the root, through a link inside it and out of it (the escapes refused, the file outside unchanged).
package main

import (
	"fmt"
	"io"
	"io/fs"
	"os"
	"path/filepath"
	"time"
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
	methods(root, filepath.Join(d, "secret"))
	sub.Close()
	root.Close()
	must(os.RemoveAll(d))
}

// methods: every os.Root method (queue 314), its result or error printed.
func methods(root *os.Root, secret string) {
	p := func(w string, err error) { fmt.Printf("%s: %v\n", w, err) }
	mode := func(n string) string {
		fi, err := root.Lstat(n)
		if err != nil {
			return err.Error()
		}
		return fi.Mode().String()
	}
	before, _ := os.Stat(secret)
	fmt.Println("root.Name:", filepath.Base(root.Name()))
	f, err := root.Create("new")
	if err == nil {
		f.WriteString("created\n")
		f.Close()
	}
	p("root.Create(new)", err)
	_, err = root.Create("esc")
	p("root.Create(esc)", err)
	p("root.WriteFile(w)", root.WriteFile("w", []byte("written\n"), 0600))
	b, err := root.ReadFile("w")
	fmt.Printf("root.ReadFile(w): %q %v\n", b, err)
	_, err = root.ReadFile("up")
	p("root.ReadFile(up)", err)
	p("root.Mkdir(m)", root.Mkdir("m", 0750))
	fmt.Println("  m:", mode("m"))
	p("root.Mkdir(m) again", root.Mkdir("m", 0750))
	p("root.Mkdir(esc/x)", root.Mkdir("esc/x", 0755))
	p("root.Mkdir(sub/loop/x)", root.Mkdir("sub/loop/x", 0755))
	fmt.Println("  x:", mode("x"))
	p("root.Mkdir(../x)", root.Mkdir("../x", 0755))
	p("root.MkdirAll(a/b/c)", root.MkdirAll("a/b/c", 0755))
	fmt.Println("  a/b/c:", mode("a/b/c"))
	p("root.MkdirAll(a/b/c) again", root.MkdirAll("a/b/c", 0755))
	p("root.MkdirAll(up/x)", root.MkdirAll("up/x", 0755))
	p("root.MkdirAll(w/x)", root.MkdirAll("w/x", 0755))
	p("root.Chmod(w, 0640)", root.Chmod("w", 0640))
	fmt.Println("  w:", mode("w"))
	p("root.Chmod(in, 0600)", root.Chmod("in", 0600))
	fmt.Println("  sub/file:", mode("sub/file"), "in:", mode("in")[0:1])
	p("root.Chmod(m, 0700)", root.Chmod("m", 0700))
	fmt.Println("  m:", mode("m"))
	p("root.Chmod(esc, 0600)", root.Chmod("esc", 0600))
	p("root.Chmod(missing, 0600)", root.Chmod("missing", 0600))
	t1, t2 := time.Unix(1000, 5), time.Unix(2000, 999999999)
	p("root.Chtimes(w)", root.Chtimes("w", t1, t2))
	if fi, err := root.Stat("w"); err == nil {
		fmt.Println("  w's mtime:", fi.ModTime().UnixNano())
	}
	p("root.Chtimes(in)", root.Chtimes("in", t1, time.Unix(3000, 0)))
	if fi, err := root.Stat("sub/file"); err == nil {
		fmt.Println("  sub/file's mtime:", fi.ModTime().Unix())
	}
	p("root.Chtimes(w, zero atime)", root.Chtimes("w", time.Time{}, time.Unix(4000, 0)))
	if fi, err := root.Stat("w"); err == nil {
		fmt.Println("  w's mtime:", fi.ModTime().Unix())
	}
	p("root.Chtimes(esc)", root.Chtimes("esc", t1, t2))
	p("root.Chtimes(missing)", root.Chtimes("missing", t1, t2))
	uid, gid := os.Getuid(), os.Getgid()
	p("root.Chown(w)", root.Chown("w", uid, gid))
	p("root.Chown(in)", root.Chown("in", -1, -1))
	p("root.Chown(esc)", root.Chown("esc", uid, gid))
	p("root.Lchown(esc)", root.Lchown("esc", uid, gid))
	p("root.Lchown(in)", root.Lchown("in", -1, -1))
	p("root.Lchown(missing)", root.Lchown("missing", uid, gid))
	p("root.Symlink(w, wl)", root.Symlink("w", "wl"))
	t, err := root.Readlink("wl")
	fmt.Println("root.Readlink(wl):", t, err)
	p("root.Symlink(w, wl) again", root.Symlink("w", "wl"))
	p("root.Symlink(w, esc/wl)", root.Symlink("w", "esc/wl"))
	p("root.Link(w, wh)", root.Link("w", "wh"))
	a, _ := root.Stat("w")
	c, _ := root.Stat("wh")
	fmt.Println("  same file:", os.SameFile(a, c))
	p("root.Link(esc, eh)", root.Link("esc", "eh"))
	p("root.Link(w, ../wh)", root.Link("w", "../wh"))
	p("root.Rename(wh, m/wh2)", root.Rename("wh", "m/wh2"))
	fmt.Println("  m/wh2:", mode("m/wh2"))
	p("root.Rename(w, ../out)", root.Rename("w", "../out"))
	p("root.Rename(esc/x, y)", root.Rename("esc/x", "y"))
	o, err := root.OpenFile("o", os.O_RDWR|os.O_CREATE|os.O_EXCL, 0604)
	if err == nil {
		o.Close()
	}
	p("root.OpenFile(o, O_CREATE|O_EXCL)", err)
	fmt.Println("  o:", mode("o"))
	_, err = root.OpenFile("o", os.O_RDWR|os.O_CREATE|os.O_EXCL, 0604)
	p("root.OpenFile(o) again", err)
	_, err = root.OpenFile("up", os.O_RDWR|os.O_APPEND, 0)
	p("root.OpenFile(up, O_APPEND)", err)
	p("root.Remove(m/wh2)", root.Remove("m/wh2"))
	p("root.Remove(m)", root.Remove("m"))
	p("root.Remove(a)", root.Remove("a"))
	p("root.Remove(missing)", root.Remove("missing"))
	p("root.Remove(esc)", root.Remove("esc"))
	fmt.Println("  esc:", mode("esc")[0:1])
	p("root.Remove(up/x)", root.Remove("up/x"))
	p("root.RemoveAll(a)", root.RemoveAll("a"))
	fmt.Println("  a:", mode("a"))
	p("root.RemoveAll(../secret)", root.RemoveAll("../secret"))
	fsys := root.FS()
	fb, err := fs.ReadFile(fsys, "sub/file")
	fmt.Printf("root.FS ReadFile(sub/file): %q %v\n", fb, err)
	_, err = fs.ReadFile(fsys, "esc")
	p("root.FS ReadFile(esc)", err)
	ents, err := fs.ReadDir(fsys, ".")
	fmt.Print("root.FS ReadDir(.):")
	for _, e := range ents {
		fmt.Print(" ", e.Name())
	}
	fmt.Println("", err)
	after, err := os.Stat(secret)
	if err == nil {
		fmt.Println("secret unchanged:", after.Mode() == before.Mode(), after.ModTime().Equal(before.ModTime()), after.Size() == before.Size())
	} else {
		fmt.Println("secret:", err)
	}
}
