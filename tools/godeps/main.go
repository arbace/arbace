// Command godeps measures the dependency graph of one printed Go package of
// Arbace's program (jrt or lang) and its strongly connected components
// (doc/STANDALONE-NOTES.md, "S5: jrt as core and library").
//
// Usage:
//
//	godeps [-goos linux] [-goarch amd64] [-edn OUT] DIR
//
// DIR holds the package's .go files as j2g and repr/go print them (for example
// target/arbace-go/work-amd64/mod/jrt). The files the build constraints of
// GOOS and GOARCH exclude are skipped.
//
// The graph's nodes are units: a translated Java source file (a file whose
// name begins with java_, javax_, jdk_, sun_ or com_, one per Java forms
// file, J2G-SPEC §4.3) is one unit with every declaration in it; a
// declaration j2g generates for a class into the package's shared files
// (j2g_classes.go: the member table, invoker, class variable and the init
// that registers them) joins the unit of its class, found by the longest
// prefix of its name that is a type of a translated file; every other
// top-level declaration (jrt's hand-written forms, the literal pool, the Up_
// conversions, the adapters) is a unit of its own, with the methods of a type
// in the type's unit (a method must be declared in its type's package). A
// unit depends on another when one of its declarations names a declaration of
// the other: exactly what makes a Go package import another when the two are
// put apart. Identifiers are resolved by go/parser's scopes, so locals,
// parameters, fields and selectors are not references.
//
// The report (standard output) gives the units, the edges, the components
// and the largest ones, their Java packages and sizes (bytes of declarations
// and AST nodes). -edn writes the graph as data for the Clojure reader:
// {:units [{:id :name :file :bytes :nodes :scc}...] :edges [[from to kind]...]}, kind 1 when the edge names values only, 2 types only, 3
// both. -types keeps only the edges that name a type: the components that remain when every
// value edge is cut (a call through a variable the higher package sets, say).
package main

import (
	"bufio"
	"flag"
	"fmt"
	"go/ast"
	"go/build"
	"go/parser"
	"go/token"
	"os"
	"path/filepath"
	"sort"
	"strings"
)

type unit struct {
	id    int
	name  string // the file for a translated file, else file:decl
	file  string
	bytes int
	nodes int
	scc   int
}

type decl struct {
	name string   // "" for init
	node ast.Node // the FuncDecl, TypeSpec or ValueSpec
	file string
	recv string // the receiver's type, for a method
	objs map[*ast.Object]bool
}

// allClasses makes every file but j2g's shared ones a translated file (arbace/lang, whose files
// are named after their Java forms files).
var allClasses bool

func translated(file string) bool {
	if allClasses {
		return !strings.HasPrefix(file, "j2g_")
	}
	for _, p := range []string{"java_", "javax_", "jdk_", "sun_", "com_"} {
		if strings.HasPrefix(file, p) {
			return true
		}
	}
	return false
}

func recvType(e ast.Expr) string {
	for {
		switch t := e.(type) {
		case *ast.StarExpr:
			e = t.X
		case *ast.IndexExpr:
			e = t.X
		case *ast.IndexListExpr:
			e = t.X
		case *ast.Ident:
			return t.Name
		default:
			return ""
		}
	}
}

func main() {
	goos := flag.String("goos", "linux", "GOOS for the files' build constraints")
	goarch := flag.String("goarch", "amd64", "GOARCH for the files' build constraints")
	ednOut := flag.String("edn", "", "write the graph as data to this file")
	top := flag.Int("top", 12, "components to detail")
	classes := flag.Bool("classes", false, "a unit per class of a translated file, not per file")
	declsOnly := flag.Bool("decls", false, "only what declarations name: signatures, fields, supertypes and interfaces' methods, not bodies or initial values")
	linkname := flag.Bool("linkname", false, "a value named costs only the types of its declaration (the edges when every value is reached by a //go:linkname)")
	why := flag.String("why", "", "A,B: print the declarations of unit A that name unit B's")
	typesOnly := flag.Bool("types", false, "only the edges that name a type (the components when every value edge is cut)")
	flag.BoolVar(&allClasses, "lang", false, "every file but j2g_*.go is a Java forms file (arbace/lang)")
	flag.Parse()
	if flag.NArg() != 1 {
		fmt.Fprintln(os.Stderr, "usage: godeps [-goos G] [-goarch A] [-edn OUT] DIR")
		os.Exit(2)
	}
	dir := flag.Arg(0)
	ctx := build.Default
	ctx.GOOS, ctx.GOARCH, ctx.CgoEnabled = *goos, *goarch, false
	ents, err := os.ReadDir(dir)
	check(err)
	fset := token.NewFileSet()
	var decls []*decl
	byName := map[string]*decl{}
	topObj := map[*ast.Object]bool{}
	files := map[string]*ast.File{}
	var names []string
	for _, e := range ents {
		n := e.Name()
		if !strings.HasSuffix(n, ".go") || strings.HasSuffix(n, "_test.go") {
			continue
		}
		if ok, err := ctx.MatchFile(dir, n); err != nil || !ok {
			continue
		}
		f, err := parser.ParseFile(fset, filepath.Join(dir, n), nil, 0)
		check(err)
		files[n] = f
		names = append(names, n)
	}
	sort.Strings(names)
	add := func(d *decl) {
		decls = append(decls, d)
		if d.name != "" && d.recv == "" {
			byName[d.name] = d
		}
	}
	for _, n := range names {
		f := files[n]
		for _, o := range f.Scope.Objects {
			topObj[o] = true
		}
		for _, dl := range f.Decls {
			switch dl := dl.(type) {
			case *ast.FuncDecl:
				d := &decl{name: dl.Name.Name, node: dl, file: n}
				if dl.Recv != nil && len(dl.Recv.List) > 0 {
					d.recv = recvType(dl.Recv.List[0].Type)
				}
				if dl.Recv == nil && dl.Name.Name == "init" {
					d.name = ""
				}
				add(d)
			case *ast.GenDecl:
				for _, s := range dl.Specs {
					switch s := s.(type) {
					case *ast.TypeSpec:
						add(&decl{name: s.Name.Name, node: s, file: n})
					case *ast.ValueSpec:
						for i, id := range s.Names {
							if id.Name == "_" {
								add(&decl{name: "", node: s, file: n})
							} else if i == 0 {
								d := &decl{name: id.Name, node: s, file: n}
								add(d)
							} else {
								byName[id.Name] = byName[s.Names[0].Name]
							}
						}
					}
				}
			}
		}
	}
	// the classes of translated files (a class's interface C_I and adapter C_Fn are its own), for
	// the generated declarations' prefixes
	transType := map[string]string{}
	for _, d := range decls {
		if _, ok := d.node.(*ast.TypeSpec); ok && translated(d.file) {
			transType[d.name] = d.file
		}
	}
	for n := range transType {
		for _, suf := range []string{"_I", "_Fn"} {
			if _, ok := transType[strings.TrimSuffix(n, suf)]; ok && strings.HasSuffix(n, suf) {
				delete(transType, n)
			}
		}
	}
	// classOf is the class a name belongs to by its longest prefix that is a class
	classOf := func(name string) (string, string) {
		for i := len(name); i > 0; i-- {
			if i == len(name) || name[i] == '_' {
				if f, ok := transType[name[:i]]; ok {
					return name[:i], f
				}
			}
		}
		return "", ""
	}
	prefixFile := func(name string) string {
		_, f := classOf(name)
		return f
	}
	unitName := func(name string) string {
		c, f := classOf(name)
		if f == "" {
			return ""
		}
		if *classes {
			return f + ":" + c
		}
		return f
	}
	// references of each declaration: refs what it names, sigs what its declaration names
	collect := func(d *decl, roots []ast.Node) []*decl {
		var out []*decl
		seen := map[*decl]bool{}
		skip := map[*ast.Ident]bool{}
		for _, root := range roots {
			ast.Inspect(root, func(n ast.Node) bool {
				switch n := n.(type) {
				case *ast.SelectorExpr:
					skip[n.Sel] = true
				case *ast.Field:
					for _, id := range n.Names {
						skip[id] = true
					}
				case *ast.CompositeLit:
					if _, isMap := n.Type.(*ast.MapType); !isMap {
						for _, el := range n.Elts {
							if kv, ok := el.(*ast.KeyValueExpr); ok {
								if id, ok := kv.Key.(*ast.Ident); ok && id.Obj == nil {
									skip[id] = true
								}
							}
						}
					}
				case *ast.FuncDecl:
					skip[n.Name] = true
				case *ast.TypeSpec:
					skip[n.Name] = true
				case *ast.ValueSpec:
					for _, id := range n.Names {
						if topObj[id.Obj] {
							skip[id] = true
						}
					}
				case *ast.LabeledStmt:
					skip[n.Label] = true
				case *ast.BranchStmt:
					if n.Label != nil {
						skip[n.Label] = true
					}
				case *ast.Ident:
					if skip[n] {
						return true
					}
					if n.Obj != nil && !topObj[n.Obj] {
						return true
					}
					if t, ok := byName[n.Name]; ok && t != d && !seen[t] {
						seen[t] = true
						out = append(out, t)
					}
				}
				return true
			})
		}
		if d.recv != "" {
			if t, ok := byName[d.recv]; ok && !seen[t] {
				out = append(out, t)
			}
		}
		return out
	}
	refs := make([][]*decl, len(decls))
	sigs := map[*decl][]*decl{}
	for i, d := range decls {
		if *declsOnly {
			refs[i] = collect(d, declRoots(d.node))
		} else {
			refs[i] = collect(d, []ast.Node{d.node})
		}
		if *linkname {
			sigs[d] = collect(d, declRoots(d.node))
		}
	}
	if *linkname {
		// a value named is reached by a linkname, which needs the types of its declaration
		for i := range decls {
			var out []*decl
			seen := map[*decl]bool{}
			for _, t := range refs[i] {
				if _, isType := t.node.(*ast.TypeSpec); isType {
					if !seen[t] {
						seen[t] = true
						out = append(out, t)
					}
					continue
				}
				for _, x := range sigs[t] {
					if !seen[x] {
						seen[x] = true
						out = append(out, x)
					}
				}
			}
			refs[i] = out
		}
	}
	// units
	var units []*unit
	unitOf := map[*decl]*unit{}
	byUnitName := map[string]*unit{}
	getUnit := func(name, file string) *unit {
		if u, ok := byUnitName[name]; ok {
			return u
		}
		u := &unit{id: len(units), name: name, file: file}
		units = append(units, u)
		byUnitName[name] = u
		return u
	}
	var place func(d *decl) *unit
	place = func(d *decl) *unit {
		if u, ok := unitOf[d]; ok {
			return u
		}
		var u *unit
		switch {
		case translated(d.file) && !*classes:
			u = getUnit(d.file, d.file)
		case translated(d.file) && d.name != "" && unitName(d.name) != "" && d.recv == "":
			u = getUnit(unitName(d.name), d.file)
		case translated(d.file) && d.recv != "" && unitName(d.recv) != "":
			u = getUnit(unitName(d.recv), d.file)
		case translated(d.file):
			u = getUnit(d.file, d.file)
		case d.recv != "":
			if t, ok := byName[d.recv]; ok {
				u = place(t)
			} else {
				u = getUnit(d.file+":"+d.recv, d.file)
			}
		case d.name != "":
			if f := prefixFile(d.name); f != "" && strings.HasPrefix(d.file, "j2g_") {
				u = getUnit(unitName(d.name), f)
			} else {
				u = getUnit(d.file+":"+d.name, d.file)
			}
		default:
			// an init or a blank var: the unit of the first class variable it names, else its own
			u = nil
		}
		if u != nil {
			unitOf[d] = u
		}
		return u
	}
	anon := 0
	for i, d := range decls {
		if place(d) == nil {
			var u *unit
			for _, t := range refs[i] {
				if strings.HasPrefix(d.file, "j2g_") && strings.HasSuffix(t.name, "_class") {
					if f := prefixFile(t.name); f != "" {
						u = getUnit(unitName(t.name), f)
						break
					}
				}
			}
			if u == nil {
				anon++
				u = getUnit(fmt.Sprintf("%s:init#%d", d.file, anon), d.file)
			}
			unitOf[d] = u
		}
	}
	for _, d := range decls {
		u := unitOf[d]
		u.bytes += fset.Position(d.node.End()).Offset - fset.Position(d.node.Pos()).Offset
		ast.Inspect(d.node, func(n ast.Node) bool {
			if n != nil {
				u.nodes++
			}
			return true
		})
	}
	// an edge's kind: 1 when a value (function, variable, constant) is named, 2 when a type is
	adj := make([]map[int]int, len(units))
	for i := range adj {
		adj[i] = map[int]int{}
	}
	nedges := 0
	for i, d := range decls {
		a := unitOf[d].id
		for _, t := range refs[i] {
			b := unitOf[t].id
			if a == b {
				continue
			}
			k := 1
			if _, ok := t.node.(*ast.TypeSpec); ok {
				k = 2
			}
			if adj[a][b] == 0 {
				nedges++
			}
			adj[a][b] |= k
		}
	}
	succ := make([][]int, len(units))
	kinds := make([][]int, len(units))
	for i := range adj {
		for b, k := range adj[i] {
			if !*typesOnly || k&2 != 0 {
				succ[i] = append(succ[i], b)
			}
		}
		sort.Ints(succ[i])
		for _, b := range succ[i] {
			kinds[i] = append(kinds[i], adj[i][b])
		}
	}
	if *why != "" {
		ab := strings.SplitN(*why, ",", 2)
		for i, d := range decls {
			if unitOf[d].name != ab[0] {
				continue
			}
			for _, t := range refs[i] {
				if unitOf[t].name == ab[1] {
					fmt.Printf("%s %s(%s) names %s %s\n", d.file, d.name, d.recv, t.file, t.name)
				}
			}
		}
		return
	}
	comps := tarjan(succ)
	for c, members := range comps {
		for _, m := range members {
			units[m].scc = c
		}
	}
	report(units, succ, comps, nedges, *top)
	if *ednOut != "" {
		writeEDN(*ednOut, units, succ, kinds)
	}
}

// declRoots are the parts of a declaration that a package must see to declare it: a function's
// receiver and signature, a type, a variable's or constant's type; not a body or an initial
// value, which can be moved to a higher package (a function variable that package sets).
func declRoots(n ast.Node) []ast.Node {
	switch n := n.(type) {
	case *ast.FuncDecl:
		if n.Recv != nil {
			return []ast.Node{n.Recv, n.Type}
		}
		return []ast.Node{n.Type}
	case *ast.ValueSpec:
		if n.Type != nil {
			return []ast.Node{n.Type}
		}
		return nil
	}
	return []ast.Node{n}
}

// tarjan returns the strongly connected components, in reverse topological
// order (a component's successors come before it), iteratively.
func tarjan(succ [][]int) [][]int {
	n := len(succ)
	index := make([]int, n)
	low := make([]int, n)
	on := make([]bool, n)
	for i := range index {
		index[i] = -1
	}
	var stack []int
	var comps [][]int
	next := 0
	type frame struct{ v, i int }
	for s := 0; s < n; s++ {
		if index[s] >= 0 {
			continue
		}
		call := []frame{{s, 0}}
		index[s], low[s] = next, next
		next++
		stack = append(stack, s)
		on[s] = true
		for len(call) > 0 {
			f := &call[len(call)-1]
			if f.i < len(succ[f.v]) {
				w := succ[f.v][f.i]
				f.i++
				if index[w] < 0 {
					index[w], low[w] = next, next
					next++
					stack = append(stack, w)
					on[w] = true
					call = append(call, frame{w, 0})
				} else if on[w] && index[w] < low[f.v] {
					low[f.v] = index[w]
				}
				continue
			}
			v := f.v
			call = call[:len(call)-1]
			if len(call) > 0 {
				p := call[len(call)-1].v
				if low[v] < low[p] {
					low[p] = low[v]
				}
			}
			if low[v] == index[v] {
				var c []int
				for {
					w := stack[len(stack)-1]
					stack = stack[:len(stack)-1]
					on[w] = false
					c = append(c, w)
					if w == v {
						break
					}
				}
				sort.Ints(c)
				comps = append(comps, c)
			}
		}
	}
	return comps
}

// javaPackage is a unit's Java package as its file names it (java_util_HashMap.go: java.util), or
// "jrt" for jrt's hand-written forms and "j2g" for the generated shared files.
func javaPackage(u *unit) string {
	if allClasses && translated(u.file) {
		return "arbace.lang"
	}
	if translated(u.file) {
		parts := strings.Split(strings.TrimSuffix(u.file, ".go"), "_")
		var p []string
		for _, s := range parts {
			if s != "" && s[0] >= 'A' && s[0] <= 'Z' {
				break
			}
			p = append(p, s)
		}
		return strings.Join(p, ".")
	}
	if strings.HasPrefix(u.file, "j2g_") {
		return "(j2g)"
	}
	return "(hand-written)"
}

func report(units []*unit, succ [][]int, comps [][]int, nedges, top int) {
	w := bufio.NewWriter(os.Stdout)
	defer w.Flush()
	total, totalNodes := 0, 0
	for _, u := range units {
		total += u.bytes
		totalNodes += u.nodes
	}
	fmt.Fprintf(w, "units %d, edges %d, components %d; %d bytes, %d nodes\n", len(units), nedges, len(comps), total, totalNodes)
	type cs struct {
		c, size, nodes, n int
	}
	var all []cs
	for c, m := range comps {
		s := cs{c: c, n: len(m)}
		for _, i := range m {
			s.size += units[i].bytes
			s.nodes += units[i].nodes
		}
		all = append(all, s)
	}
	sort.Slice(all, func(i, j int) bool { return all[i].nodes > all[j].nodes })
	nontrivial := 0
	for _, s := range all {
		if s.n > 1 {
			nontrivial++
		}
	}
	fmt.Fprintf(w, "components of more than one unit: %d\n", nontrivial)
	for k, s := range all {
		if k >= top {
			break
		}
		// what the component reaches (itself included)
		reach := map[int]bool{}
		var work []int
		for _, i := range comps[s.c] {
			reach[i] = true
			work = append(work, i)
		}
		for len(work) > 0 {
			v := work[len(work)-1]
			work = work[:len(work)-1]
			for _, x := range succ[v] {
				if !reach[x] {
					reach[x] = true
					work = append(work, x)
				}
			}
		}
		rb, rn := 0, 0
		for i := range reach {
			rb += units[i].bytes
			rn += units[i].nodes
		}
		fmt.Fprintf(w, "\ncomponent %d: %d units, %d bytes (%.1f%%), %d nodes (%.1f%%); with what it reaches %d units, %d bytes, %d nodes\n",
			s.c, s.n, s.size, 100*float64(s.size)/float64(total), s.nodes, 100*float64(s.nodes)/float64(totalNodes), len(reach), rb, rn)
		byPkg := map[string][3]int{}
		for _, i := range comps[s.c] {
			p := javaPackage(units[i])
			v := byPkg[p]
			v[0]++
			v[1] += units[i].bytes
			v[2] += units[i].nodes
			byPkg[p] = v
		}
		var ps []string
		for p := range byPkg {
			ps = append(ps, p)
		}
		sort.Slice(ps, func(i, j int) bool { return byPkg[ps[i]][2] > byPkg[ps[j]][2] })
		for _, p := range ps {
			v := byPkg[p]
			fmt.Fprintf(w, "  %-40s %5d units %10d bytes %9d nodes\n", p, v[0], v[1], v[2])
		}
	}
}

func writeEDN(path string, units []*unit, succ [][]int, kinds [][]int) {
	f, err := os.Create(path)
	check(err)
	w := bufio.NewWriter(f)
	fmt.Fprintln(w, "{:units [")
	for _, u := range units {
		fmt.Fprintf(w, " {:id %d :name %q :file %q :package %q :bytes %d :nodes %d :scc %d}\n", u.id, u.name, u.file, javaPackage(u), u.bytes, u.nodes, u.scc)
	}
	fmt.Fprintln(w, "]\n :edges [")
	for a, ss := range succ {
		for j, b := range ss {
			fmt.Fprintf(w, " [%d %d %d]", a, b, kinds[a][j])
		}
		if len(ss) > 0 {
			fmt.Fprintln(w)
		}
	}
	fmt.Fprintln(w, "]}")
	check(w.Flush())
	check(f.Close())
}

func check(err error) {
	if err != nil {
		fmt.Fprintln(os.Stderr, "godeps:", err)
		os.Exit(1)
	}
}
