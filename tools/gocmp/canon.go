package main

// gocmp reprint -canon: a candidate respelled in every way the tree level
// normalizes, to show at the export and code levels that the normalizations
// do not change the program:
//
//   - imports: one declaration per import, in reverse order;
//   - var and type groups split into one declaration per spec, a const group
//     of one spec ungrouped;
//   - field groups (a, b T) expanded to (a T, b T), except in type parameter
//     lists and generic declarations;
//   - parentheses around every operand of a binary expression, except the
//     operands of && and ||;
//   - integer literals in decimal, raw strings interpreted;
//   - composite literal types that may be elided written out.

import (
	"go/ast"
	"go/constant"
	"go/token"
	"os"
	"strconv"
	"strings"
)

// canonSkip lists respellings to leave out (GOCMP_CANON_SKIP, comma-separated:
// imports, groups, fields, parens, literals, elision), to attribute
// differences to one of them.
var canonSkip = func() map[string]bool {
	m := map[string]bool{}
	for _, k := range strings.Split(os.Getenv("GOCMP_CANON_SKIP"), ",") {
		m[k] = true
	}
	return m
}()

// canonAlso lists the respellings that are not neutral for gc, to apply too
// (GOCMP_CANON_ALSO, comma-separated): logical (parentheses around the
// operands of && and ||), generic (field groups of generic declarations
// expanded).
var canonAlso = func() map[string]bool {
	m := map[string]bool{}
	for _, k := range strings.Split(os.Getenv("GOCMP_CANON_ALSO"), ",") {
		m[k] = true
	}
	return m
}()

func canonicalize(f *ast.File, pl *placement) {
	var imports, rest []ast.Decl
	var importDirectives []string
	for _, d := range f.Decls {
		if g, ok := d.(*ast.GenDecl); ok && g.Tok == token.IMPORT {
			importDirectives = append(importDirectives, pl.beforeDecl[d]...)
			for _, s := range g.Specs {
				importDirectives = append(importDirectives, pl.beforeSpec[s]...)
				imports = append(imports, &ast.GenDecl{Tok: token.IMPORT, Specs: []ast.Spec{s}})
			}
			importDirectives = append(importDirectives, pl.endGroup[d]...)
			continue
		}
		rest = append(rest, canonDecl(d, pl)...)
	}
	for i, j := 0, len(imports)-1; i < j && !canonSkip["imports"]; i, j = i+1, j-1 {
		imports[i], imports[j] = imports[j], imports[i]
	}
	if len(imports) > 0 {
		pl.beforeDecl[imports[0]] = importDirectives
	}
	f.Decls = append(imports, rest...)
	kept := keptFieldLists(f)
	if canonAlso["generic"] {
		kept = map[*ast.FieldList]bool{}
	}
	// unions of type terms (~int | ~uint) are binary expressions that do
	// not take parentheses
	unions := map[*ast.BinaryExpr]bool{}
	markUnions := func(n ast.Node) {
		ast.Inspect(n, func(n ast.Node) bool {
			if b, ok := n.(*ast.BinaryExpr); ok {
				unions[b] = true
			}
			return true
		})
	}
	ast.Inspect(f, func(n ast.Node) bool {
		switch n := n.(type) {
		case *ast.InterfaceType:
			markUnions(n)
		case *ast.FuncType:
			if n.TypeParams != nil {
				markUnions(n.TypeParams)
			}
		case *ast.TypeSpec:
			if n.TypeParams != nil {
				markUnions(n.TypeParams)
			}
		}
		return true
	})
	ast.Inspect(f, func(n ast.Node) bool {
		switch n := n.(type) {
		case *ast.BlockStmt:
			n.List = canonStmts(n.List, pl)
		case *ast.CaseClause:
			n.Body = canonStmts(n.Body, pl)
		case *ast.CommClause:
			n.Body = canonStmts(n.Body, pl)
		case *ast.FieldList:
			if !canonSkip["fields"] && !kept[n] {
				n.List = normFields(n.List)
			}
		case *ast.BinaryExpr:
			if unions[n] || canonSkip["parens"] {
				break
			}
			// not around the operands of && and ||: gc's dead code
			// elimination (staticBool, noder/writer.go) does not look
			// through parentheses (ROUNDTRIP.md, "Known gaps")
			if (n.Op == token.LAND || n.Op == token.LOR) && !canonAlso["logical"] {
				break
			}
			n.X = &ast.ParenExpr{X: n.X}
			n.Y = &ast.ParenExpr{X: n.Y}
		case *ast.BasicLit:
			if !canonSkip["literals"] {
				canonLit(n)
			}
		case *ast.CompositeLit:
			if n.Type != nil && !canonSkip["elision"] {
				unelide(n, n.Type)
			}
		}
		return true
	})
}

// canonDecl splits a var or type group (unless directives sit at its end)
// and ungroups a const group of one spec.
func canonDecl(d ast.Decl, pl *placement) []ast.Decl {
	g, ok := d.(*ast.GenDecl)
	if !ok || !g.Lparen.IsValid() || canonSkip["groups"] {
		return []ast.Decl{d}
	}
	if g.Tok == token.CONST {
		if len(g.Specs) == 1 && len(pl.beforeSpec[g.Specs[0]]) == 0 && len(pl.endGroup[g]) == 0 {
			g.Lparen = token.NoPos
		}
		return []ast.Decl{d}
	}
	if len(g.Specs) == 0 || len(pl.endGroup[d]) > 0 {
		return []ast.Decl{d}
	}
	var out []ast.Decl
	for i, s := range g.Specs {
		nd := &ast.GenDecl{Tok: g.Tok, Specs: []ast.Spec{s}}
		if i == 0 {
			pl.beforeDecl[nd] = pl.beforeDecl[d]
		}
		pl.beforeDecl[nd] = append(pl.beforeDecl[nd], pl.beforeSpec[s]...)
		delete(pl.beforeSpec, s)
		out = append(out, nd)
	}
	return out
}

func canonStmts(list []ast.Stmt, pl *placement) []ast.Stmt {
	var out []ast.Stmt
	for _, s := range list {
		if ds, ok := s.(*ast.DeclStmt); ok {
			for _, d := range canonDecl(ds.Decl, pl) {
				out = append(out, &ast.DeclStmt{Decl: d})
			}
			continue
		}
		out = append(out, s)
	}
	return out
}

func canonLit(l *ast.BasicLit) {
	switch l.Kind {
	case token.INT:
		v := constant.MakeFromLiteral(l.Value, token.INT, 0)
		if v.Kind() == constant.Int {
			l.Value = v.ExactString()
		}
	case token.STRING:
		if strings.HasPrefix(l.Value, "`") {
			l.Value = strconv.Quote(strings.ReplaceAll(l.Value[1:len(l.Value)-1], "\r", ""))
		}
	}
}

// unelide writes out the element types elided in lit (of syntactic type typ),
// where the type is known syntactically (the inverse of elide in tree.go).
func unelide(lit *ast.CompositeLit, typ ast.Expr) {
	var key, elt ast.Expr
	switch t := unparen(typ).(type) {
	case *ast.ArrayType:
		elt = t.Elt
	case *ast.MapType:
		key, elt = t.Key, t.Value
	default:
		return
	}
	one := func(x ast.Expr, et ast.Expr) ast.Expr {
		c, ok := x.(*ast.CompositeLit)
		if !ok || c.Type != nil || et == nil {
			return x
		}
		switch unparen(et).(type) {
		case *ast.ArrayType, *ast.MapType, *ast.StructType, *ast.StarExpr:
		default:
			// a name may stand for a pointer type or a type parameter,
			// whose literals cannot be written out
			return x
		}
		if star, ok := unparen(et).(*ast.StarExpr); ok {
			c.Type = star.X
			unelide(c, star.X)
			return &ast.UnaryExpr{Op: token.AND, X: c}
		}
		c.Type = et
		unelide(c, et)
		return c
	}
	for i, e := range lit.Elts {
		if kv, ok := e.(*ast.KeyValueExpr); ok {
			kv.Key = one(kv.Key, key)
			kv.Value = one(kv.Value, elt)
		} else {
			lit.Elts[i] = one(e, elt)
		}
	}
}
