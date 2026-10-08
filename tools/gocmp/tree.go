package main

// Level 1: the trees.
//
// Both sources are parsed with go/parser and compared node by node after the
// normalizations listed in doc/go/ROUNDTRIP.md ("Level 1"). Each normalization
// identifies two spellings that the language defines to mean the same program;
// anything else that differs is reported.

import (
	"fmt"
	"go/ast"
	"go/build/constraint"
	"go/constant"
	"go/parser"
	"go/token"
	"os"
	"reflect"
	"sort"
	"strconv"
	"strings"
)

// srcFile is one parsed Go file.
type srcFile struct {
	path string
	src  []byte
	fset *token.FileSet
	f    *ast.File
}

func parseFile(path string) (*srcFile, error) {
	src, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	fset := token.NewFileSet()
	f, err := parser.ParseFile(fset, path, src, parser.ParseComments|parser.SkipObjectResolution)
	if err != nil {
		return nil, err
	}
	return &srcFile{path: path, src: src, fset: fset, f: f}, nil
}

// isDirective reports whether a comment is one the round trip must keep: a
// //go: directive (compiler directives, //go:build, //go:embed, //go:generate,
// ...) or a legacy // +build constraint. //line directives only change
// positions, which no level compares (ROUNDTRIP.md, "Positions").
func isDirective(text string) bool {
	return strings.HasPrefix(text, "//go:") || strings.HasPrefix(text, "// +build")
}

// declKeys names every top-level function and spec in source order as
// "kind name#n", n counting earlier occurrences of the same "kind name". The
// keys do not depend on how declarations are grouped.
func declKeys(f *ast.File) map[ast.Node]string {
	keys := map[ast.Node]string{}
	seen := map[string]int{}
	add := func(n ast.Node, k string) {
		keys[n] = fmt.Sprintf("%s#%d", k, seen[k])
		seen[k]++
	}
	for _, d := range f.Decls {
		switch d := d.(type) {
		case *ast.FuncDecl:
			k := "func " + d.Name.Name
			if d.Recv != nil && len(d.Recv.List) > 0 {
				k = "func (" + exprString(d.Recv.List[0].Type) + ")." + d.Name.Name
			}
			add(d, k)
		case *ast.GenDecl:
			for _, s := range d.Specs {
				if d.Tok == token.IMPORT {
					// imports are a set: a directive among them is
					// anchored to them all
					keys[s] = "imports"
					continue
				}
				add(s, d.Tok.String()+" "+specName(s))
			}
			if len(d.Specs) == 0 {
				add(d, d.Tok.String()+" ()")
			}
		}
	}
	return keys
}

func specName(s ast.Spec) string {
	switch s := s.(type) {
	case *ast.ImportSpec:
		return s.Path.Value
	case *ast.ValueSpec:
		var names []string
		for _, n := range s.Names {
			names = append(names, n.Name)
		}
		return strings.Join(names, ",")
	case *ast.TypeSpec:
		return s.Name.Name
	}
	return "?"
}

// exprString prints an expression compactly, for keys and messages.
func exprString(x ast.Expr) string {
	var b strings.Builder
	var w func(x ast.Expr)
	w = func(x ast.Expr) {
		switch x := x.(type) {
		case *ast.Ident:
			b.WriteString(x.Name)
		case *ast.StarExpr:
			b.WriteString("*")
			w(x.X)
		case *ast.ParenExpr:
			w(x.X)
		case *ast.SelectorExpr:
			w(x.X)
			b.WriteString("." + x.Sel.Name)
		case *ast.IndexExpr:
			w(x.X)
			b.WriteString("[")
			w(x.Index)
			b.WriteString("]")
		case *ast.IndexListExpr:
			w(x.X)
			b.WriteString("[")
			for i, e := range x.Indices {
				if i > 0 {
					b.WriteString(",")
				}
				w(e)
			}
			b.WriteString("]")
		case *ast.BasicLit:
			b.WriteString(x.Value)
		default:
			fmt.Fprintf(&b, "%T", x)
		}
	}
	w(x)
	return b.String()
}

// fileDirectives returns the file's directives as sorted "anchor\ttext"
// lines. The anchor is the declaration the directive precedes ("header"
// before the package clause, "eof" after the last declaration), or "inside"
// the declaration that contains it.
func fileDirectives(f *ast.File) []string {
	keys := declKeys(f)
	var out []string
	for _, g := range f.Comments {
		for _, c := range g.List {
			text := strings.TrimRight(c.Text, " \t\r")
			if !isDirective(text) {
				continue
			}
			if x, err := constraint.Parse(text); err == nil && constraint.IsGoBuild(text) {
				// a build constraint is an expression: a&&b is a && b
				text = "//go:build " + x.String()
			}
			out = append(out, directiveAnchor(f, keys, c.Slash)+"\t"+text)
		}
	}
	sort.Strings(out)
	return out
}

func directiveAnchor(f *ast.File, keys map[ast.Node]string, p token.Pos) string {
	if p < f.Package {
		return "header"
	}
	for _, d := range f.Decls {
		if d.End() <= p {
			continue
		}
		first := func() string {
			if g, ok := d.(*ast.GenDecl); ok && len(g.Specs) > 0 {
				return keys[g.Specs[0]]
			}
			return keys[d]
		}
		if p < d.Pos() {
			return first()
		}
		if g, ok := d.(*ast.GenDecl); ok && g.Lparen.IsValid() && p > g.Lparen {
			for _, s := range g.Specs {
				if s.Pos() > p {
					return keys[s]
				}
			}
			return "end of group " + first()
		}
		return "inside " + first()
	}
	return "eof"
}

// fileImports returns the file's imports as a sorted multiset of
// "name path" lines.
func fileImports(f *ast.File) []string {
	var out []string
	for _, s := range f.Imports {
		name := ""
		if s.Name != nil {
			name = s.Name.Name
		}
		path, err := strconv.Unquote(s.Path.Value)
		if err != nil {
			path = s.Path.Value
		}
		out = append(out, name+" "+path)
	}
	sort.Strings(out)
	return out
}

// normalize rewrites the tree in place (after directives and imports have
// been taken from it):
//   - import declarations are removed (compared as a multiset);
//   - var and type declarations are split into one declaration per spec, and
//     empty groups dropped; a const group of one spec counts as ungrouped
//     (const groups of several specs are kept: iota and implicit repetition
//     depend on them);
//   - field groups (a, b T) become one field per name, except in type
//     parameter lists and generic declarations;
//   - empty statements are dropped from statement lists;
//   - composite literal types that may be elided are elided.
//
// Parentheses are skipped by the comparison itself.
func normalize(f *ast.File) {
	f.Decls = normDecls(f.Decls, true)
	kept := keptFieldLists(f)
	ast.Inspect(f, func(n ast.Node) bool {
		switch n := n.(type) {
		case *ast.BlockStmt:
			n.List = normStmts(n.List)
		case *ast.CaseClause:
			n.Body = normStmts(n.Body)
		case *ast.CommClause:
			n.Body = normStmts(n.Body)
		case *ast.FieldList:
			if !kept[n] {
				n.List = normFields(n.List)
			}
		case *ast.CompositeLit:
			if n.Type != nil {
				elide(n, n.Type)
			}
		}
		return true
	})
}

// keptFieldLists returns the field lists whose groups are not expanded: type
// parameter lists, and every field list of a generic declaration (a function
// with type parameters or a generic receiver, a type with type parameters).
// There, (a, b []T) and (a []T, b []T) are the same program, but gc gives the
// function a different dictionary: each written []T is a type of its own, and
// derived types are listed by identity (ROUNDTRIP.md, "Level 1").
func keptFieldLists(f *ast.File) map[*ast.FieldList]bool {
	m := map[*ast.FieldList]bool{}
	all := func(n ast.Node) {
		ast.Inspect(n, func(n ast.Node) bool {
			if fl, ok := n.(*ast.FieldList); ok {
				m[fl] = true
			}
			return true
		})
	}
	ast.Inspect(f, func(n ast.Node) bool {
		switch n := n.(type) {
		case *ast.FuncDecl:
			generic := n.Type.TypeParams != nil
			if n.Recv != nil && len(n.Recv.List) > 0 {
				t := n.Recv.List[0].Type
				if s, ok := t.(*ast.StarExpr); ok {
					t = s.X
				}
				switch unparen(t).(type) {
				case *ast.IndexExpr, *ast.IndexListExpr:
					generic = true
				}
			}
			if generic {
				all(n)
				return false
			}
		case *ast.TypeSpec:
			if n.TypeParams != nil {
				all(n)
				return false
			}
		case *ast.FuncType:
			if n.TypeParams != nil {
				m[n.TypeParams] = true
			}
		}
		return true
	})
	return m
}

func normDecls(list []ast.Decl, top bool) []ast.Decl {
	var out []ast.Decl
	for _, d := range list {
		g, ok := d.(*ast.GenDecl)
		if !ok {
			out = append(out, d)
			continue
		}
		switch {
		case g.Tok == token.IMPORT && top:
			// compared as a multiset
		case len(g.Specs) == 0:
			// var (), const (), type (): no declarations
		case g.Tok == token.CONST && len(g.Specs) > 1:
			out = append(out, g)
		default:
			for _, s := range g.Specs {
				out = append(out, &ast.GenDecl{TokPos: g.TokPos, Tok: g.Tok, Specs: []ast.Spec{s}})
			}
		}
	}
	return out
}

func normStmts(list []ast.Stmt) []ast.Stmt {
	var out []ast.Stmt
	for _, s := range list {
		switch s := s.(type) {
		case *ast.EmptyStmt:
			continue
		case *ast.DeclStmt:
			for _, d := range normDecls([]ast.Decl{s.Decl}, false) {
				out = append(out, &ast.DeclStmt{Decl: d})
			}
			continue
		}
		out = append(out, s)
	}
	return out
}

func normFields(list []*ast.Field) []*ast.Field {
	var out []*ast.Field
	for _, fl := range list {
		if len(fl.Names) <= 1 {
			out = append(out, fl)
			continue
		}
		for _, n := range fl.Names {
			out = append(out, &ast.Field{Names: []*ast.Ident{n}, Type: fl.Type, Tag: fl.Tag})
		}
	}
	return out
}

func unparen(x ast.Expr) ast.Expr {
	for {
		p, ok := x.(*ast.ParenExpr)
		if !ok {
			return x
		}
		x = p.X
	}
}

// elide removes the element types of the composite literals inside lit (of
// syntactic type typ) that the spec allows to elide: T in T{...} and &T{...}
// where the element (or key) type is T or *T. Only types known syntactically
// are used: literals of an array, slice or map type written out; the element
// type of a named type is not looked up.
func elide(lit *ast.CompositeLit, typ ast.Expr) {
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
		if et == nil {
			return x
		}
		switch e := unparen(x).(type) {
		case *ast.CompositeLit:
			if e.Type != nil && sameExpr(e.Type, et) {
				e.Type = nil
			}
			if e.Type == nil {
				elide(e, et)
			}
			return x
		case *ast.UnaryExpr:
			star, ok := unparen(et).(*ast.StarExpr)
			if !ok || e.Op != token.AND {
				return x
			}
			c, ok := unparen(e.X).(*ast.CompositeLit)
			if !ok || c.Type == nil || !sameExpr(c.Type, star.X) {
				return x
			}
			c.Type = nil
			elide(c, star.X)
			return c
		}
		return x
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

func sameExpr(a, b ast.Expr) bool {
	return cmpValue(reflect.ValueOf(&a).Elem(), reflect.ValueOf(&b).Elem()) == nil
}

// treeDiff is the first difference between two trees.
type treeDiff struct {
	path       []string // outermost first, built while unwinding
	detail     string
	posA, posB token.Pos
}

func (d *treeDiff) at(s string) *treeDiff {
	d.path = append(d.path, s)
	return d
}

var (
	posType     = reflect.TypeFor[token.Pos]()
	commentType = reflect.TypeFor[*ast.CommentGroup]()
	objectType  = reflect.TypeFor[*ast.Object]()
	scopeType   = reflect.TypeFor[*ast.Scope]()
	exprType    = reflect.TypeFor[ast.Expr]()
	basicType   = reflect.TypeFor[ast.BasicLit]()
	nodeType    = reflect.TypeFor[ast.Node]()
)

// skipField lists the fields that carry no meaning for the program: comments,
// parser bookkeeping, and the derived lists of ast.File.
var skipField = map[string]bool{
	"Doc": true, "Comment": true, "Comments": true, "Imports": true, "Unresolved": true,
	"Incomplete": true, "Implicit": true, "Obj": true, "Scope": true,
}

// posMeaning lists the position fields whose validity is syntax: an alias
// declaration (type A = B), a variadic call f(x...), a parenthesized group.
var posMeaning = map[string]bool{"Assign": true, "Ellipsis": true, "Lparen": true}

func cmpValue(x, y reflect.Value) *treeDiff {
	if x.Type() == exprType {
		if !x.IsNil() {
			x = reflect.ValueOf(unparen(x.Interface().(ast.Expr)))
		}
		if !y.IsNil() {
			y = reflect.ValueOf(unparen(y.Interface().(ast.Expr)))
		}
	}
	if x.Type() != y.Type() { // after unparen: different node types
		d := &treeDiff{detail: fmt.Sprintf("%s vs %s", showValue(x), showValue(y))}
		setPos(d, x, y)
		return d
	}
	switch x.Kind() {
	case reflect.Interface, reflect.Pointer:
		if x.IsNil() || y.IsNil() {
			if x.IsNil() != y.IsNil() {
				return &treeDiff{detail: fmt.Sprintf("%s vs %s", showValue(x), showValue(y))}
			}
			return nil
		}
		if x.Kind() == reflect.Interface {
			x, y = x.Elem(), y.Elem()
			if x.Type() != y.Type() {
				d := &treeDiff{detail: fmt.Sprintf("%s vs %s", showValue(x), showValue(y))}
				setPos(d, x, y)
				return d
			}
			return cmpValue(x, y)
		}
		if x.Type().Elem() == basicType {
			a, b := x.Interface().(*ast.BasicLit), y.Interface().(*ast.BasicLit)
			if !sameLit(a, b) {
				return &treeDiff{detail: fmt.Sprintf("%s %s vs %s %s", a.Kind, a.Value, b.Kind, b.Value), posA: a.Pos(), posB: b.Pos()}
			}
			return nil
		}
		d := cmpValue(x.Elem(), y.Elem())
		if d != nil {
			setPos(d, x, y)
		}
		return d
	case reflect.Struct:
		t := x.Type()
		for i := range t.NumField() {
			f := t.Field(i)
			if !f.IsExported() || skipField[f.Name] {
				continue
			}
			switch f.Type {
			case commentType, objectType, scopeType:
				continue
			case posType:
				if posMeaning[f.Name] {
					a, b := token.Pos(x.Field(i).Int()), token.Pos(y.Field(i).Int())
					if a.IsValid() != b.IsValid() {
						return (&treeDiff{detail: fmt.Sprintf("%s present: %v vs %v", f.Name, a.IsValid(), b.IsValid())}).at(f.Name)
					}
				}
				continue
			}
			if d := cmpValue(x.Field(i), y.Field(i)); d != nil {
				return d.at(f.Name)
			}
		}
		return nil
	case reflect.Slice:
		if x.Len() != y.Len() {
			d := &treeDiff{detail: fmt.Sprintf("%d vs %d elements", x.Len(), y.Len())}
			n := min(x.Len(), y.Len())
			for i := range n {
				if e := cmpValue(x.Index(i), y.Index(i)); e != nil {
					e.detail += fmt.Sprintf(" (and %d vs %d elements)", x.Len(), y.Len())
					return e.at(fmt.Sprintf("[%d]%s", i, nodeLabel(x.Index(i))))
				}
			}
			if n < x.Len() {
				d.detail += "; first extra in A: " + nodeLabel(x.Index(n))
				setPos(d, x.Index(n), reflect.Value{})
			} else {
				d.detail += "; first extra in B: " + nodeLabel(y.Index(n))
				setPos(d, reflect.Value{}, y.Index(n))
			}
			return d
		}
		for i := range x.Len() {
			if d := cmpValue(x.Index(i), y.Index(i)); d != nil {
				return d.at(fmt.Sprintf("[%d]%s", i, nodeLabel(x.Index(i))))
			}
		}
		return nil
	}
	if !x.Equal(y) {
		return &treeDiff{detail: fmt.Sprintf("%v vs %v", x, y)}
	}
	return nil
}

func setPos(d *treeDiff, x, y reflect.Value) {
	if d.posA == 0 && x.IsValid() && x.Type().Implements(nodeType) && !x.IsNil() {
		d.posA = x.Interface().(ast.Node).Pos()
	}
	if d.posB == 0 && y.IsValid() && y.Type().Implements(nodeType) && !y.IsNil() {
		d.posB = y.Interface().(ast.Node).Pos()
	}
}

func showValue(v reflect.Value) string {
	if !v.IsValid() || ((v.Kind() == reflect.Interface || v.Kind() == reflect.Pointer) && v.IsNil()) {
		return "nil"
	}
	if v.Kind() == reflect.Interface {
		v = v.Elem()
	}
	return nodeLabel(v)
}

// nodeLabel describes a node briefly: its type and, if it has one, its name.
func nodeLabel(v reflect.Value) string {
	if v.Kind() == reflect.Interface {
		if v.IsNil() {
			return "(nil)"
		}
		v = v.Elem()
	}
	if v.Kind() != reflect.Pointer || v.IsNil() {
		return ""
	}
	name := strings.TrimPrefix(v.Type().String(), "*ast.")
	switch n := v.Interface().(type) {
	case *ast.FuncDecl:
		return "(" + name + " " + n.Name.Name + ")"
	case *ast.GenDecl:
		if len(n.Specs) > 0 {
			return "(" + name + " " + n.Tok.String() + " " + specName(n.Specs[0]) + ")"
		}
	case *ast.Ident:
		return "(" + name + " " + n.Name + ")"
	case *ast.BasicLit:
		return "(" + name + " " + n.Value + ")"
	case *ast.Field:
		if len(n.Names) > 0 {
			return "(" + name + " " + n.Names[0].Name + ")"
		}
	}
	return "(" + name + ")"
}

// sameLit compares literals by kind and value: 0x10 and 16, "a" and `a`,
// 1_000 and 1000 are the same constant. The kind stays significant ('a' is
// an untyped rune, 97 an untyped int; 1e3 is a float).
func sameLit(a, b *ast.BasicLit) bool {
	if a.Kind != b.Kind {
		return false
	}
	if a.Value == b.Value {
		return true
	}
	va, vb := litValue(a), litValue(b)
	if va.Kind() == constant.Unknown || vb.Kind() == constant.Unknown {
		return false
	}
	return constant.Compare(va, token.EQL, vb)
}

func litValue(l *ast.BasicLit) constant.Value {
	v := l.Value
	if l.Kind == token.STRING && strings.HasPrefix(v, "`") {
		// carriage returns are discarded from raw strings (spec, "String literals")
		v = strings.ReplaceAll(v, "\r", "")
	}
	return constant.MakeFromLiteral(v, l.Kind, 0)
}

// treeResult compares two files.
func compareFiles(a, b *srcFile) string {
	var out []string
	if ia, ib := fileImports(a.f), fileImports(b.f); !equalStrings(ia, ib) {
		out = append(out, "imports differ:\n"+listDiff(ia, ib))
	}
	if da, db := fileDirectives(a.f), fileDirectives(b.f); !equalStrings(da, db) {
		out = append(out, "directives differ (anchor, text):\n"+listDiff(da, db))
	}
	normalize(a.f)
	normalize(b.f)
	if d := cmpValue(reflect.ValueOf(a.f), reflect.ValueOf(b.f)); d != nil {
		var path strings.Builder
		path.WriteString("File")
		for i := len(d.path) - 1; i >= 0; i-- {
			if !strings.HasPrefix(d.path[i], "[") {
				path.WriteString(".")
			}
			path.WriteString(d.path[i])
		}
		msg := fmt.Sprintf("at %s\n  %s", path.String(), d.detail)
		msg += "\n  A " + srcLine(a, d.posA)
		msg += "\n  B " + srcLine(b, d.posB)
		out = append(out, msg)
	}
	return strings.Join(out, "\n")
}

func srcLine(s *srcFile, p token.Pos) string {
	if !p.IsValid() {
		return s.path + ": (no position)"
	}
	pos := s.fset.PositionFor(p, false) // physical, not after //line directives
	f := s.fset.File(p)
	start := f.Offset(f.LineStart(pos.Line))
	end := start
	for end < len(s.src) && s.src[end] != '\n' {
		end++
	}
	line := strings.TrimSpace(string(s.src[start:end]))
	if len(line) > 160 {
		line = line[:160] + "..."
	}
	return fmt.Sprintf("%s:%d:%d: %s", pos.Filename, pos.Line, pos.Column, line)
}

func equalStrings(a, b []string) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

// listDiff shows the lines of two sorted lists that are not in both.
func listDiff(a, b []string) string {
	var out []string
	i, j := 0, 0
	for i < len(a) || j < len(b) {
		switch {
		case j >= len(b) || (i < len(a) && a[i] < b[j]):
			out = append(out, "  - "+a[i])
			i++
		case i >= len(a) || b[j] < a[i]:
			out = append(out, "  + "+b[j])
			j++
		default:
			i++
			j++
		}
	}
	if len(out) > 20 {
		out = append(out[:20], fmt.Sprintf("  ... %d more", len(out)-20))
	}
	return strings.Join(out, "\n")
}
