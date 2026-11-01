package main

import (
	"bytes"
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
	if !strings.HasPrefix(out.String(), "{:cxxdump 1 :main \"m.cpp\"}\n{:id \"0x5\" :kind \"NamespaceDecl\"") {
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
