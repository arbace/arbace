// Command golines compares the lines of two Go files as g2c's printer promises them in
// positions mode :lines (doc/go/SPEC.md §10.1, §12.3): the line, as gc sees it (after
// //line directives), of every declaration, spec, struct field, statement, case clause and
// function closing brace, in source order.
//
// Usage: golines ORIGINAL PRINTED
//
// Exit status 0 when the sequences are equal, 1 when they differ (the first difference is
// printed), 2 on an error.
package main

import (
	"fmt"
	"go/ast"
	"go/parser"
	"go/token"
	"os"
)

type item struct {
	kind string
	line int
}

func lines(path string) ([]item, error) {
	fset := token.NewFileSet()
	f, err := parser.ParseFile(fset, path, nil, parser.SkipObjectResolution)
	if err != nil {
		return nil, err
	}
	var out []item
	add := func(kind string, p token.Pos) {
		out = append(out, item{kind, fset.PositionFor(p, true).Line})
	}
	var stack []ast.Node
	ast.Inspect(f, func(n ast.Node) bool {
		if n == nil {
			top := stack[len(stack)-1]
			stack = stack[:len(stack)-1]
			switch top := top.(type) {
			case *ast.FuncDecl:
				if top.Body != nil {
					add("} "+top.Name.Name, top.Body.Rbrace)
				}
			case *ast.FuncLit:
				add("} func literal", top.Body.Rbrace)
			}
			return false
		}
		stack = append(stack, n)
		switch n := n.(type) {
		case *ast.FuncDecl:
			add("func "+n.Name.Name, n.Pos())
		case *ast.GenDecl:
			if n.Tok != token.IMPORT {
				if n.Lparen.IsValid() || n.Pos() == n.Specs[0].Pos() {
					add(n.Tok.String(), n.Pos())
				}
			} else {
				stack = stack[:len(stack)-1]
				return false
			}
		case *ast.ValueSpec:
			add("spec "+n.Names[0].Name, n.Pos())
		case *ast.TypeSpec:
			add("type "+n.Name.Name, n.Pos())
		case *ast.Field:
			// struct fields only (one per line: same-line groups may be regrouped, N3)
			if _, ok := stack[len(stack)-3].(*ast.StructType); ok {
				if l := fset.PositionFor(n.Pos(), true).Line; len(out) == 0 || out[len(out)-1] != (item{"field", l}) {
					add("field", n.Pos())
				}
			}
		case *ast.EmptyStmt:
		case *ast.CaseClause:
			add("case", n.Pos())
		case *ast.CommClause:
			add("comm", n.Pos())
		case *ast.BlockStmt:
		case ast.Stmt:
			add(fmt.Sprintf("%T", n)[5:], n.Pos())
		}
		return true
	})
	return out, nil
}

func main() {
	if len(os.Args) != 3 {
		fmt.Fprintln(os.Stderr, "usage: golines ORIGINAL PRINTED")
		os.Exit(2)
	}
	a, err := lines(os.Args[1])
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(2)
	}
	b, err := lines(os.Args[2])
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(2)
	}
	for i := 0; i < len(a) && i < len(b); i++ {
		if a[i] != b[i] {
			fmt.Printf("item %d: original %s at line %d, printed %s at line %d\n", i, a[i].kind, a[i].line, b[i].kind, b[i].line)
			os.Exit(1)
		}
	}
	if len(a) != len(b) {
		fmt.Printf("original has %d items, printed %d\n", len(a), len(b))
		os.Exit(1)
	}
	fmt.Printf("lines: pass (%d items)\n", len(a))
}
