package main

import (
	"go/parser"
	"go/token"
	"strings"
	"testing"
)

func parseString(t *testing.T, name, src string) *srcFile {
	t.Helper()
	fset := token.NewFileSet()
	f, err := parser.ParseFile(fset, name, src, parser.ParseComments|parser.SkipObjectResolution)
	if err != nil {
		t.Fatalf("%s: %v", name, err)
	}
	return &srcFile{path: name, src: []byte(src), fset: fset, f: f}
}

// Each pair is two spellings; same says whether the tree level identifies them.
var treePairs = []struct {
	name string
	a, b string
	same bool
}{
	{"parens", "var x = (a + b) * c", "var x = ((a + (b)) * (c))", true},
	{"precedence", "var x = (a + b) * c", "var x = a + b*c", false},
	{"var group", "var (\n\ta = 1\n\tb = 2\n)", "var a = 1\nvar b = 2", true},
	{"type group", "type (\n\tA int\n\tB string\n)", "type A int\ntype B string", true},
	{"const group of one", "const (\n\tA = iota\n)", "const A = iota", true},
	{"const group", "const (\n\tA = iota\n\tB\n)", "const A = iota\nconst B = iota", false},
	{"empty group", "var ()\nvar x int", "var x int", true},
	{"field group", "func f(a, b int) (x, y string) { return }", "func f(a int, b int) (x string, y string) { return }", true},
	{"struct field group", "type T struct{ a, b int `t` }", "type T struct {\n\ta int `t`\n\tb int `t`\n}", true},
	{"type param group", "func f[K, V any]() {}", "func f[K any, V any]() {}", false},
	{"generic field group", "func f[T any](a, b []T) {}", "func f[T any](a []T, b []T) {}", false},
	{"result parens", "func f() (int) { return 0 }", "func f() int { return 0 }", true},
	{"int literal", "var x = 0x10 + 1_000", "var x = 16 + 1000", true},
	{"string literal", "var s = `a\"b`", "var s = \"a\\\"b\"", true},
	{"rune vs int", "var r = 'a'", "var r = 97", false},
	{"float vs int", "var f = 1e3", "var f = 1000", false},
	{"elision", "var x = []T{T{1}, T{2}}\nvar m = map[K]*V{K{}: &V{}}", "var x = []T{{1}, {2}}\nvar m = map[K]*V{{}: {}}", true},
	{"named elision", "var x = Ts{T{1}}", "var x = Ts{{1}}", false},
	{"imports", "import (\n\t\"b\"\n\tx \"a\"\n)", "import x \"a\"\nimport \"b\"", true},
	{"import name", "import \"a\"", "import y \"a\"", false},
	{"empty statements", "func f() { a(); ; b() }", "func f() {\n\ta()\n\tb()\n}", true},
	{"alias", "type A = int", "type A int", false},
	{"variadic", "var _ = f(x...)", "var _ = f(x)", false},
	{"swap", "var _ = a - b", "var _ = b - a", false},
	{"comments", "// doc\nvar x = 1 // c", "var x = /* c */ 1", true},
	{"directive", "//go:noinline\nfunc f() {}\nfunc g() {}", "func f() {}\n\n//go:noinline\nfunc g() {}", false},
	{"directive kept", "//go:noinline\n// doc\nfunc f() {}", "// other\n//go:noinline\nfunc f() {}", true},
	{"build constraint", "//go:build a&&(b||c)\n\npackage p", "//go:build a && (b || c)\n\npackage p", true},
	{"build constraint differs", "//go:build a && b\n\npackage p", "//go:build a || b\n\npackage p", false},
	{"for", "func f() { for ;; {} }", "func f() { for {} }", true},
}

func TestTreePairs(t *testing.T) {
	for _, p := range treePairs {
		src := func(s string) string {
			if strings.Contains(s, "package p") {
				return s
			}
			return "package p\n\n" + s
		}
		a := parseString(t, "a.go", src(p.a))
		b := parseString(t, "b.go", src(p.b))
		d := compareFiles(a, b)
		if (d == "") != p.same {
			t.Errorf("%s: same=%v, want %v; difference: %s", p.name, d == "", p.same, d)
		}
	}
}

func TestFlattenSameLayout(t *testing.T) {
	// two spellings the tree level identifies have the same canonical layout,
	// up to the tokens themselves: compare line structure (one line per line)
	a := "package p\n\nimport (\n\t\"a\"\n\t\"b\"\n)\n\nvar (\n\tx, y int\n)\n\ntype T struct{ a, b []int }\n\nfunc f(a, b int) int {\n\tif (a) > (b) { return a }\n\treturn b\n}\n"
	b := "package p\n\nimport \"b\"\nimport \"a\"\n\nvar x, y int\n\ntype T struct {\n\ta []int\n\tb []int\n}\n\nfunc f(a int, b int) int {\n\tif a > b {\n\t\treturn a\n\t}\n\treturn b\n}\n"
	la := strings.Count(string(flatten("f.go", []byte(a))), "\n")
	lb := strings.Count(string(flatten("f.go", []byte(b))), "\n")
	if la != lb {
		t.Errorf("canonical layouts have %d and %d lines:\n%s\n%s", la, lb, flatten("f.go", []byte(a)), flatten("f.go", []byte(b)))
	}
}
