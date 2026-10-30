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
