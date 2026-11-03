// Command fmacheck keeps Go from fusing a floating-point multiply and add (FMA) in a package whose results must be
// bit for bit another implementation's: the musl libm port go/arbace/libm (doc/haskell/HASKELL-EVAL-SURVEY.md,
// "As built: HE4d, HE4e (write-ahead)"; the Mac's review 189, F1). Go's specification lets an implementation fuse
// x*y + z, and gc does on arm64 (and on amd64 at GOAMD64=v3), but never across an explicit conversion: float64(x*y) + z
// rounds the product first. So every floating-point product of the package is written as a conversion of itself, but
// one that is the operand of another product or of a quotient (its result rounded before any add).
//
//	fmacheck DIR       lists each floating-point product (a*b, or a *= b) of DIR's package that is not the operand
//	                   of a conversion to its own type, as FILE:LINE:COL; exit status 1 when there is one
//	fmacheck -w DIR    rewrites them: a*b as T(a*b), a *= b as a = T(a * (b)), T float64 or float32
//
// It type-checks the package from source (GOROOT the toolchain's), with no dependency beyond Go's standard library.
package main

import (
	"fmt"
	"go/ast"
	"go/importer"
	"go/parser"
	"go/token"
	"go/types"
	"os"
	"path/filepath"
	"sort"
	"strings"
)

type edit struct {
	off  int
	text string
	end  bool // an insertion at a node's end goes after one at a node's start at the same offset
}

func main() {
	write := false
	args := os.Args[1:]
	if len(args) > 0 && args[0] == "-w" {
		write, args = true, args[1:]
	}
	if len(args) != 1 {
		fmt.Fprintln(os.Stderr, "usage: fmacheck [-w] DIR")
		os.Exit(2)
	}
	dir := args[0]
	fset := token.NewFileSet()
	pkgs, err := parser.ParseDir(fset, dir, func(fi os.FileInfo) bool { return !strings.HasSuffix(fi.Name(), "_test.go") }, parser.ParseComments)
	if err != nil {
		fmt.Fprintln(os.Stderr, "fmacheck:", err)
		os.Exit(2)
	}
	bad := 0
	for _, pkg := range pkgs {
		var files []*ast.File
		var names []string
		for n, f := range pkg.Files {
			names = append(names, n)
			_ = f
		}
		sort.Strings(names)
		for _, n := range names {
			files = append(files, pkg.Files[n])
		}
		info := &types.Info{Types: map[ast.Expr]types.TypeAndValue{}}
		conf := types.Config{Importer: importer.ForCompiler(fset, "source", nil)}
		if _, err := conf.Check(pkg.Name, fset, files, info); err != nil {
			fmt.Fprintln(os.Stderr, "fmacheck:", err)
			os.Exit(2)
		}
		floatName := func(e ast.Expr) string {
			tv, ok := info.Types[e]
			if !ok || tv.Value != nil { // a constant product is exact Go arithmetic: never fused
				return ""
			}
			b, ok := tv.Type.Underlying().(*types.Basic)
			if !ok {
				return ""
			}
			switch b.Kind() {
			case types.Float64:
				return "float64"
			case types.Float32:
				return "float32"
			}
			return ""
		}
		for _, n := range names {
			f := pkg.Files[n]
			var edits []edit
			// conv: the products that are a conversion's operand already, or the operand of a product or a quotient
			// (whose own result is what an add could fuse with)
			conv := map[ast.Expr]bool{}
			unparen := func(a ast.Expr) ast.Expr {
				for {
					p, ok := a.(*ast.ParenExpr)
					if !ok {
						return a
					}
					a = p.X
				}
			}
			ast.Inspect(f, func(x ast.Node) bool {
				if b, ok := x.(*ast.BinaryExpr); ok && (b.Op == token.MUL || b.Op == token.QUO) && floatName(b) != "" {
					conv[unparen(b.X)] = true
					conv[unparen(b.Y)] = true
				}
				if c, ok := x.(*ast.CallExpr); ok && len(c.Args) == 1 {
					if id, ok := c.Fun.(*ast.Ident); ok && (id.Name == "float64" || id.Name == "float32") {
						if tv, ok := info.Types[c.Fun]; ok && tv.IsType() {
							a := c.Args[0]
							for {
								p, ok := a.(*ast.ParenExpr)
								if !ok {
									break
								}
								a = p.X
							}
							conv[a] = true
						}
					}
				}
				return true
			})
			ast.Inspect(f, func(x ast.Node) bool {
				switch e := x.(type) {
				case *ast.BinaryExpr:
					if e.Op != token.MUL {
						return true
					}
					t := floatName(e)
					if t == "" || conv[e] {
						return true
					}
					p := fset.Position(e.Pos())
					if write {
						edits = append(edits, edit{off: p.Offset, text: t + "("}, edit{off: fset.Position(e.End()).Offset, text: ")", end: true})
					} else {
						fmt.Printf("%s:%d:%d: a %s product not converted: %s\n", n, p.Line, p.Column, t, src(fset, e))
					}
					bad++
				case *ast.AssignStmt:
					if e.Tok != token.MUL_ASSIGN || len(e.Lhs) != 1 {
						return true
					}
					t := floatName(e.Lhs[0])
					if t == "" {
						return true
					}
					p := fset.Position(e.Pos())
					if write {
						l := src(fset, e.Lhs[0])
						r := src(fset, e.Rhs[0])
						edits = append(edits, edit{off: p.Offset, text: "\x00" + fmt.Sprintf("%s = %s(%s * (%s))", l, t, l, r)},
							edit{off: fset.Position(e.End()).Offset, text: "\x01", end: true})
					} else {
						fmt.Printf("%s:%d:%d: a %s *= not converted: %s\n", n, p.Line, p.Column, t, src(fset, e))
					}
					bad++
				}
				return true
			})
			if write && len(edits) > 0 {
				b, err := os.ReadFile(n)
				if err != nil {
					fmt.Fprintln(os.Stderr, "fmacheck:", err)
					os.Exit(2)
				}
				if err := os.WriteFile(n, apply(b, edits), 0o644); err != nil {
					fmt.Fprintln(os.Stderr, "fmacheck:", err)
					os.Exit(2)
				}
			}
		}
	}
	if write {
		fmt.Fprintf(os.Stderr, "fmacheck: %d products converted in %s\n", bad, filepath.Clean(dir))
		return
	}
	if bad > 0 {
		fmt.Fprintf(os.Stderr, "fmacheck: %d floating-point products of %s not converted (fmacheck -w)\n", bad, filepath.Clean(dir))
		os.Exit(1)
	}
	fmt.Fprintf(os.Stderr, "fmacheck: every floating-point product of %s converted\n", filepath.Clean(dir))
}

var srcs = map[string][]byte{}

func src(fset *token.FileSet, n ast.Node) string {
	p, q := fset.Position(n.Pos()), fset.Position(n.End())
	b, ok := srcs[p.Filename]
	if !ok {
		b, _ = os.ReadFile(p.Filename)
		srcs[p.Filename] = b
	}
	return string(b[p.Offset:q.Offset])
}

// apply inserts the edits; a statement's replacement (\x00TEXT at its start, \x01 at its end) drops the text between.
func apply(b []byte, es []edit) []byte {
	sort.SliceStable(es, func(i, j int) bool {
		if es[i].off != es[j].off {
			return es[i].off < es[j].off
		}
		return !es[i].end && es[j].end
	})
	var out []byte
	pos, skip := 0, false
	for _, e := range es {
		if !skip {
			out = append(out, b[pos:e.off]...)
		}
		pos = e.off
		switch {
		case strings.HasPrefix(e.text, "\x00"):
			out = append(out, e.text[1:]...)
			skip = true
		case e.text == "\x01":
			skip = false
		default:
			if !skip {
				out = append(out, e.text...)
			}
		}
	}
	return append(out, b[pos:]...)
}
