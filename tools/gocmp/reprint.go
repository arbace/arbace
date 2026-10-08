package main

// gocmp reprint: a candidate made without forms, the stand-in for g2c's
// printer. Every Go file is parsed, its comments are dropped except the
// directives, every position is reset, and go/printer prints it again; the
// directives are put back before the declaration they preceded. The result is
// what a printer working from a tree without positions or comments produces.

import (
	"bytes"
	"fmt"
	"go/ast"
	"go/format"
	"go/parser"
	"go/printer"
	"go/token"
	"io/fs"
	"os"
	"path/filepath"
	"reflect"
	"runtime"
	"strings"
	"sync"
	"sync/atomic"
)

func mainReprint(args []string) int {
	canon := false
	if len(args) > 0 && args[0] == "-canon" {
		canon, args = true, args[1:]
	}
	if len(args) != 2 {
		fmt.Fprintln(os.Stderr, `usage: gocmp reprint [-canon] SRCROOT DSTROOT

Reprints every .go file under SRCROOT into DSTROOT (same relative paths);
files that do not parse or print are copied as they are, and listed.
With -canon, the trees are first respelled in every way the tree level
normalizes (canon.go).`)
		return 2
	}
	src, dst := args[0], args[1]
	var paths []string
	err := filepath.WalkDir(src, func(p string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if !d.IsDir() && strings.HasSuffix(p, ".go") {
			paths = append(paths, p)
		}
		return nil
	})
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		return 2
	}
	var next, copied, failed atomic.Int64
	var mu sync.Mutex
	var wg sync.WaitGroup
	for range runtime.NumCPU() {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for {
				i := int(next.Add(1)) - 1
				if i >= len(paths) {
					return
				}
				p := paths[i]
				rel, _ := filepath.Rel(src, p)
				out := filepath.Join(dst, rel)
				b, err := os.ReadFile(p)
				if err == nil {
					var r []byte
					r, err = reprint(p, b, canon)
					if err != nil {
						copied.Add(1)
						mu.Lock()
						fmt.Fprintf(os.Stderr, "copied: %s: %s\n", p, firstLines(err.Error(), 1))
						mu.Unlock()
						r = b
						err = nil
					}
					if err = os.MkdirAll(filepath.Dir(out), 0o755); err == nil {
						err = os.WriteFile(out, r, 0o644)
					}
				}
				if err != nil {
					failed.Add(1)
					mu.Lock()
					fmt.Fprintln(os.Stderr, err)
					mu.Unlock()
				}
			}
		}()
	}
	wg.Wait()
	fmt.Printf("reprinted %d files, %d copied as they are (they do not parse or print), %d errors\n",
		int64(len(paths))-copied.Load()-failed.Load(), copied.Load(), failed.Load())
	if failed.Load() > 0 {
		return 1
	}
	return 0
}

// placement records where the directives of a file go.
type placement struct {
	header, eof []string
	beforeDecl  map[ast.Decl][]string
	beforeSpec  map[ast.Spec][]string
	endGroup    map[ast.Decl][]string
}

func placeDirectives(f *ast.File) *placement {
	pl := &placement{beforeDecl: map[ast.Decl][]string{}, beforeSpec: map[ast.Spec][]string{}, endGroup: map[ast.Decl][]string{}}
	for _, g := range f.Comments {
		for _, c := range g.List {
			text := strings.TrimRight(c.Text, " \t\r")
			if !isDirective(text) {
				continue
			}
			p := c.Slash
			if p < f.Package {
				pl.header = append(pl.header, text)
				continue
			}
			placed := false
			for _, d := range f.Decls {
				if d.End() <= p {
					continue
				}
				placed = true
				if gd, ok := d.(*ast.GenDecl); ok && p > d.Pos() && gd.Lparen.IsValid() && p > gd.Lparen {
					done := false
					for _, s := range gd.Specs {
						if s.Pos() > p {
							pl.beforeSpec[s] = append(pl.beforeSpec[s], text)
							done = true
							break
						}
					}
					if !done {
						pl.endGroup[d] = append(pl.endGroup[d], text)
					}
				} else {
					// before the declaration, or inside a function: put
					// before it (the tree level reports the move)
					pl.beforeDecl[d] = append(pl.beforeDecl[d], text)
				}
				break
			}
			if !placed {
				pl.eof = append(pl.eof, text)
			}
		}
	}
	return pl
}

// resetPositions drops the comments attached to nodes and sets every
// position in the tree to "unknown", except those
// whose presence is syntax (an alias's =, a variadic call's ..., a group's
// parenthesis), which become position 1.
func resetPositions(n any) {
	var walk func(v reflect.Value)
	walk = func(v reflect.Value) {
		switch v.Kind() {
		case reflect.Pointer, reflect.Interface:
			if !v.IsNil() {
				walk(v.Elem())
			}
		case reflect.Slice:
			for i := range v.Len() {
				walk(v.Index(i))
			}
		case reflect.Struct:
			t := v.Type()
			for i := range t.NumField() {
				f := t.Field(i)
				if !f.IsExported() {
					continue
				}
				switch f.Type {
				case commentType:
					// doc and line comments: go/printer prints them
					v.Field(i).SetZero()
					continue
				case objectType, scopeType:
					continue
				case posType:
					fv := v.Field(i)
					if posMeaning[f.Name] && token.Pos(fv.Int()).IsValid() {
						fv.SetInt(1)
					} else {
						fv.SetInt(0)
					}
					continue
				}
				walk(v.Field(i))
			}
		}
	}
	walk(reflect.ValueOf(n))
}

func reprint(path string, src []byte, canon bool) ([]byte, error) {
	fset := token.NewFileSet()
	f, err := parser.ParseFile(fset, path, src, parser.ParseComments|parser.SkipObjectResolution)
	if err != nil {
		return nil, err
	}
	pl := placeDirectives(f)
	if canon {
		canonicalize(f, pl)
	}
	f.Comments = nil
	resetPositions(f)
	cfg := printer.Config{Mode: printer.UseSpaces | printer.TabIndent, Tabwidth: 8}
	var b bytes.Buffer
	lines := func(ds []string) {
		for _, d := range ds {
			b.WriteString(d + "\n")
		}
	}
	if len(pl.header) > 0 {
		lines(pl.header)
		b.WriteString("\n")
	}
	fmt.Fprintf(&b, "package %s\n", f.Name.Name)
	pfset := token.NewFileSet()
	for _, d := range f.Decls {
		b.WriteString("\n")
		lines(pl.beforeDecl[d])
		gd, isGen := d.(*ast.GenDecl)
		inner := false
		if isGen {
			for _, s := range gd.Specs {
				if len(pl.beforeSpec[s]) > 0 {
					inner = true
				}
			}
			inner = inner || len(pl.endGroup[d]) > 0
		}
		if !inner {
			if err := cfg.Fprint(&b, pfset, d); err != nil {
				return nil, err
			}
			b.WriteString("\n")
			continue
		}
		fmt.Fprintf(&b, "%s (\n", gd.Tok)
		for _, s := range gd.Specs {
			lines(pl.beforeSpec[s])
			if err := cfg.Fprint(&b, pfset, s); err != nil {
				return nil, err
			}
			b.WriteString("\n")
		}
		lines(pl.endGroup[d])
		b.WriteString(")\n")
	}
	if len(pl.eof) > 0 {
		b.WriteString("\n")
		lines(pl.eof)
	}
	out, err := format.Source(b.Bytes())
	if err != nil {
		return nil, fmt.Errorf("%s: reprinted source does not parse: %v", path, err)
	}
	return out, nil
}
