package main

import (
	"bytes"
	"os"
	"strings"
	"testing"
)

// A dump with clang's delta encoding: the header's declaration sets the file, the first main-file declaration
// names it, the second leaves file and line out.
const dump = `{"id":"0x1","kind":"TranslationUnitDecl","loc":{},"range":{"begin":{},"end":{}},"inner":[
{"id":"0x2","kind":"TypedefDecl","loc":{},"name":"__builtin_va_list"},
{"id":"0x3","kind":"NamespaceDecl","loc":{"offset":10,"file":"/h/a.h","line":3,"col":1,"tokLen":9},"name":"std","inner":[
 {"id":"0x4","kind":"FunctionDecl","loc":{"offset":20,"line":4,"col":2,"tokLen":1},"name":"f"}]},
{"id":"0x5","kind":"NamespaceDecl","loc":{"offset":5,"file":"m.cpp","line":1,"col":11,"tokLen":1},"name":"n","inner":[
 {"id":"0x6","kind":"FunctionDecl","loc":{"offset":15,"line":2,"col":5,"tokLen":1},"name":"g","isImplicit":true},
 {"id":"0x7","kind":"FunctionDecl","loc":{"offset":25,"line":3,"col":5,"tokLen":1},"name":"h"}]},
{"id":"0x8","kind":"FunctionDecl","loc":{"offset":40,"line":6,"col":5,"tokLen":1},"name":"main"}]}`

func TestFilter(t *testing.T) {
	var out bytes.Buffer
	_, _, err := run(strings.NewReader(dump), &out, "m.cpp", false, false)
	if err != nil {
		t.Fatal(err)
	}
	lines := strings.Split(strings.TrimSpace(out.String()), "\n")
	if len(lines) != 3 { // header, namespace n, main
		t.Fatalf("lines: %d\n%s", len(lines), out.String())
	}
	if strings.Contains(out.String(), `"name":"g"`) || strings.Contains(out.String(), "a.h") {
		t.Fatalf("implicit or header declaration kept:\n%s", out.String())
	}
	if !strings.Contains(lines[2], `"file":"m.cpp","line":6`) && !strings.Contains(lines[2], `"line":6,"col":5,"tokLen":1,"file":"m.cpp"`) {
		t.Fatalf("main's location not absolute: %s", lines[2])
	}
}

func TestEdn(t *testing.T) {
	var out bytes.Buffer
	if _, _, err := run(strings.NewReader(dump), &out, "m.cpp", false, true); err != nil {
		t.Fatal(err)
	}
	if !strings.HasPrefix(out.String(), "{:cxxdump 1 :main \"m.cpp\"}\n{:id \"0x1\" :kind \"NamespaceDecl\"") { // clang's 0x5, renumbered
		t.Fatalf("edn: %s", out.String())
	}
	if got := ednString("a\"b\\c\n\x01"); got != `"a\"b\\c\n\u0001"` {
		t.Fatalf("ednString: %s", got)
	}
}

// A header's node whose inner nodes carry the location changes: skipped as it streams, the main file's node after it
// still gets its absolute file and line.
const dump2 = `{"id":"0x1","kind":"TranslationUnitDecl","loc":{},"range":{"begin":{},"end":{}},"inner":[
{"id":"0x2","kind":"FunctionDecl","loc":{"offset":10,"file":"/h/a.h","line":3,"col":1,"tokLen":1},"name":"h","inner":[
 {"id":"0x3","kind":"CompoundStmt","range":{"begin":{"offset":12,"file":"m.cpp","line":9,"col":1,"tokLen":1},"end":{"offset":13,"col":2,"tokLen":1}}}]},
{"id":"0x4","kind":"ClassTemplateDecl","loc":{"offset":40,"line":6,"col":5,"tokLen":1},"name":"W","inner":[
 {"id":"0x5","kind":"CXXRecordDecl","loc":{"offset":40,"col":5,"tokLen":1},"name":"W"},
 {"id":"0x6","kind":"ClassTemplateSpecializationDecl","loc":{"offset":40,"col":5,"tokLen":1},"name":"W","inner":[
  {"kind":"TemplateArgument","type":{"qualType":"int"},"inner":[{"id":"0x7","kind":"BuiltinType","type":{"qualType":"int"}}]},
  {"id":"0x8","kind":"FieldDecl","loc":{"offset":44,"col":9,"tokLen":1},"name":"v"}]}]}]}`

func TestSkipAndSummary(t *testing.T) {
	var out bytes.Buffer
	if _, _, err := runOpts(strings.NewReader(dump2), &out, options{main: "m.cpp", implicit: true, edn: false}); err != nil {
		t.Fatal(err)
	}
	s := out.String()
	if strings.Contains(s, `"name":"h"`) {
		t.Fatalf("the header's node was kept:\n%s", s)
	}
	if !strings.Contains(s, `"line":6`) || !strings.Contains(s, `"file":"m.cpp"`) {
		t.Fatalf("the location after a skipped node is not absolute:\n%s", s)
	}
	if strings.Contains(s, "FieldDecl") || strings.Contains(s, "BuiltinType") || !strings.Contains(s, "TemplateArgument") {
		t.Fatalf("the instantiation is not summarized:\n%s", s)
	}
}

func TestNorm(t *testing.T) {
	var out bytes.Buffer
	if _, _, err := runOpts(strings.NewReader(dump2), &out, options{main: "m.cpp", implicit: true, edn: true, norm: true}); err != nil {
		t.Fatal(err)
	}
	if strings.Contains(out.String(), ":id ") || strings.Contains(out.String(), ":loc ") {
		t.Fatalf("norm kept ids or locations:\n%s", out.String())
	}
}

// CP77 (b): the normal form has no FullComment, whatever the declaration it is attached to.
func TestNormFullComment(t *testing.T) {
	d := &obj{keys: []string{"kind", "name", "inner"}, vals: []any{"FunctionDecl", "f", []any{
		&obj{keys: []string{"kind", "inner"}, vals: []any{"FullComment", []any{&obj{keys: []string{"kind"}, vals: []any{"ParagraphComment"}}}}},
		&obj{keys: []string{"kind"}, vals: []any{"CompoundStmt"}}}}}
	n := normalize(d).(*obj)
	inner := n.vals[2].([]any)
	if len(inner) != 1 || inner[0].(*obj).str("kind") != "CompoundStmt" {
		t.Fatalf("FullComment kept: %v", inner)
	}
}

// A key clang writes twice (UsingType's "type") is written once under its name and then as "type-2".
func TestRepeatedKey(t *testing.T) {
	o := &obj{keys: []string{"kind", "type", "decl", "type"}, vals: []any{"UsingType", "a", "b", "c"}}
	var b strings.Builder
	writeEdn(&b, o)
	want := `{:kind "UsingType" :type "a" :decl "b" :type-2 "c"}`
	if b.String() != want {
		t.Fatalf("got %s want %s", b.String(), want)
	}
}

// The ids are renumbered by first appearance across the dump's forms: a reference keeps its node's number, and two runs whose
// addresses differ write the same text.
func TestRenumber(t *testing.T) {
	ids := map[string]string{}
	a := &obj{keys: []string{"id", "kind", "inner"}, vals: []any{"0x7f10", "FunctionDecl", []any{&obj{keys: []string{"id", "referencedDecl"},
		vals: []any{"0x7f30", &obj{keys: []string{"id", "name"}, vals: []any{"0x7f10", "0x10"}}}}}}}
	b := &obj{keys: []string{"id", "previousDecl", "value"}, vals: []any{"0x7f20", "0x7f30", "10"}}
	renumber(a, ids)
	renumber(b, ids)
	var w strings.Builder
	writeEdn(&w, a)
	writeEdn(&w, b)
	want := `{:id "0x1" :kind "FunctionDecl" :inner [{:id "0x2" :referencedDecl {:id "0x1" :name "0x3"}}]}{:id "0x4" :previousDecl "0x2" :value "10"}`
	if w.String() != want {
		t.Fatalf("got %s want %s", w.String(), want)
	}
}

// Under CXXDUMP_ROOT a path in the arguments is keyed as @ROOT@/...: the same unit has one key in every checkout.
func TestRootKey(t *testing.T) {
	dir := t.TempDir()
	src := dir + "/a.cpp"
	os.WriteFile(src, []byte("int main() {}\n"), 0o644)
	key := func(root string) string {
		t.Setenv("CXXDUMP_ROOT", root)
		k, err := job{clang: "clang++-none", args: []string{"-iquote", root + "/x", src}}.key()
		if err != nil {
			t.Fatal(err)
		}
		return k
	}
	clangID = "clang version test"
	defer func() { clangID = "" }()
	if a, b := key("/r1"), key("/r2"); a != b {
		t.Fatalf("keys differ by root: %s %s", a, b)
	}
	if rootAbs("@ROOT@/x/h.h") != "/r2/x/h.h" || rootRel("/r2/x/h.h") != "@ROOT@/x/h.h" {
		t.Fatalf("rootRel/rootAbs: %s %s", rootAbs("@ROOT@/x/h.h"), rootRel("/r2/x/h.h"))
	}
}
