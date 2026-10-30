// NH3 (doc/go/NET-HTTP-SURVEY.md, "As built: NH2 (write-ahead)"): an http.FileSystem and its http.File of
// the program's own types, served by compiled http.FileServer: Open, then the File's Stat, Readdir,
// Read, Seek and Close called by compiled code through their stubs, with fs.FileInfo values of the
// program's type; conditional and range requests over them; http.FS over an fs.FS of the program's.
package main

import (
	"errors"
	"fmt"
	"io"
	"io/fs"
	"net/http"
	"net/http/httptest"
	"sort"
	"strings"
	"time"
)

var stamp = time.Date(2026, 10, 30, 12, 0, 0, 0, time.UTC)

type info struct {
	name string
	size int64
	dir  bool
}

func (i info) Name() string { return i.name }
func (i info) Size() int64  { return i.size }
func (i info) Mode() fs.FileMode {
	if i.dir {
		return fs.ModeDir | 0o555
	}
	return 0o444
}
func (i info) ModTime() time.Time { return stamp }
func (i info) IsDir() bool        { return i.dir }
func (i info) Sys() any           { return nil }

// memFS holds files by path ("/" the root directory); calls counts the methods compiled code called.
type memFS struct {
	files map[string]string
	calls map[string]int
}

type memFile struct {
	fs   *memFS
	name string
	r    *strings.Reader
	dir  bool
}

func (m *memFS) Open(name string) (http.File, error) {
	m.calls["Open"]++
	if name == "/" {
		return &memFile{fs: m, name: "/", dir: true, r: strings.NewReader("")}, nil
	}
	s, ok := m.files[name]
	if !ok {
		return nil, fs.ErrNotExist
	}
	return &memFile{fs: m, name: name, r: strings.NewReader(s)}, nil
}

func (f *memFile) Read(p []byte) (int, error) { f.fs.calls["Read"]++; return f.r.Read(p) }
func (f *memFile) Seek(off int64, whence int) (int64, error) {
	f.fs.calls["Seek"]++
	return f.r.Seek(off, whence)
}
func (f *memFile) Close() error { f.fs.calls["Close"]++; return nil }
func (f *memFile) Stat() (fs.FileInfo, error) {
	f.fs.calls["Stat"]++
	if f.dir {
		return info{name: "/", dir: true}, nil
	}
	return info{name: strings.TrimPrefix(f.name, "/"), size: f.r.Size()}, nil
}
func (f *memFile) Readdir(count int) ([]fs.FileInfo, error) {
	f.fs.calls["Readdir"]++
	if !f.dir {
		return nil, errors.New("not a directory")
	}
	var names []string
	for n := range f.fs.files {
		names = append(names, n)
	}
	sort.Strings(names)
	var out []fs.FileInfo
	for _, n := range names {
		out = append(out, info{name: strings.TrimPrefix(n, "/"), size: int64(len(f.fs.files[n]))})
	}
	return out, nil
}

// oneFS is an fs.FS of the program's own (http.FS wraps it): one file.
type oneFS struct{}

type oneFile struct{ r *strings.Reader }

func (oneFS) Open(name string) (fs.File, error) {
	if name != "only.txt" {
		return nil, &fs.PathError{Op: "open", Path: name, Err: fs.ErrNotExist}
	}
	return oneFile{strings.NewReader("the only file\n")}, nil
}
func (f oneFile) Read(p []byte) (int, error)   { return f.r.Read(p) }
func (f oneFile) Close() error                 { return nil }
func (f oneFile) Stat() (fs.FileInfo, error)   { return info{name: "only.txt", size: f.r.Size()}, nil }
func (f oneFile) Seek(o int64, w int) (int64, error) { return f.r.Seek(o, w) }

func main() {
	m := &memFS{files: map[string]string{"/a.txt": "alpha\n", "/b.txt": "bravo bravo\n"}, calls: map[string]int{}}
	mux := http.NewServeMux()
	mux.Handle("/", http.FileServer(m))
	mux.Handle("/one/", http.StripPrefix("/one", http.FileServer(http.FS(oneFS{}))))
	ts := httptest.NewServer(mux)
	defer ts.Close()
	do := func(p string, hdr ...string) {
		req, _ := http.NewRequest("GET", ts.URL+p, nil)
		for i := 0; i+1 < len(hdr); i += 2 {
			req.Header.Set(hdr[i], hdr[i+1])
		}
		res, err := http.DefaultClient.Do(req)
		if err != nil {
			fmt.Println(err)
			return
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Printf("%s %d %q type=%q range=%q\n", p, res.StatusCode, b, res.Header.Get("Content-Type"), res.Header.Get("Content-Range"))
	}
	do("/a.txt")
	do("/b.txt", "Range", "bytes=6-")
	do("/a.txt", "If-Modified-Since", stamp.Add(time.Hour).Format(http.TimeFormat))
	do("/missing.txt")
	do("/")
	do("/one/only.txt")
	do("/one/other.txt")
	var ks []string
	for k := range m.calls {
		if m.calls[k] > 0 {
			ks = append(ks, k)
		}
	}
	sort.Strings(ks)
	fmt.Println("methods called:", strings.Join(ks, " "))
}
