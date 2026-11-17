package main

// ORS9 (a) (doc/ORACLE-RECORDS-SURVEY.md, "As built: the tooling and C"): the installed headers from a pack, as
// arbace.repr.c.pp reads them. ARBACE_C_SYSROOT names a directory holding them under their absolute paths with roots.txt
// (test/c/host/search.txt's directories; bin/lib/repr-c/sysroot.bash): cc's file system (Config.FS) opens a name under one of
// those roots from it alone; a name the pack lacks opens as an unreadable directory, so that cc's fallback to os.Open and
// os.Stat (cpp.go's hasFile, scanner.go's source) never reaches the host's file. Positions keep the name. "/" or unset:
// the host's files.

import (
	"errors"
	"io/fs"
	"os"
	"path/filepath"
	"strings"
	"time"
)

type sysrootFS struct {
	dir   string
	roots []string
}

func newSysrootFS() fs.FS {
	d := strings.TrimRight(os.Getenv("ARBACE_C_SYSROOT"), "/")
	if d == "" {
		return nil
	}
	b, err := os.ReadFile(filepath.Join(d, "roots.txt"))
	check(err)
	s := &sysrootFS{dir: d}
	for _, l := range strings.Split(string(b), "\n") {
		if l = strings.TrimRight(strings.TrimSpace(l), "/"); l != "" {
			s.roots = append(s.roots, l)
		}
	}
	return s
}

func (s *sysrootFS) under(name string) bool {
	for _, r := range s.roots {
		if name == r || strings.HasPrefix(name, r+"/") {
			return true
		}
	}
	return false
}

func (s *sysrootFS) Open(name string) (fs.File, error) {
	if !filepath.IsAbs(name) || !s.under(filepath.Clean(name)) {
		return nil, fs.ErrNotExist // cc's own fallback: os.Open, the host's (a corpus file, its own directory)
	}
	f, err := os.Open(s.dir + filepath.Clean(name))
	if err != nil {
		return absentFile{filepath.Base(name)}, nil
	}
	return f, nil
}

type absentFile struct{ name string }

func (a absentFile) Stat() (fs.FileInfo, error) { return a, nil }
func (a absentFile) Read([]byte) (int, error) {
	return 0, errors.New("not in the headers pack (ARBACE_C_SYSROOT)")
}
func (a absentFile) Close() error       { return nil }
func (a absentFile) Name() string       { return a.name }
func (a absentFile) Size() int64        { return 0 }
func (a absentFile) Mode() fs.FileMode  { return fs.ModeDir | 0o555 }
func (a absentFile) ModTime() time.Time { return time.Time{} }
func (a absentFile) IsDir() bool        { return true }
func (a absentFile) Sys() any           { return nil }

var sysroot = newSysrootFS()
