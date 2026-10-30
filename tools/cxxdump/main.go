// cxxdump filters clang's -ast-dump=json of one translation unit down to the declarations of the main file
// (doc/cpp/SPEC.md §11, "tools/cxxdump"; doc/cpp/CPP-SURVEY.md CP2 (a), "As built: CX1").
//
//	cxxdump [-main NAME] [-implicit=false] [-format edn|json] [-stdin] [-o FILE] -- CLANG-ARGS... FILE
//	    runs clang++ -Xclang -ast-dump=json -fsyntax-only on FILE and filters its output as it streams
//	cxxdump -stdin -main NAME < dump.json
//	    filters a dump already made (the test of the filter)
//
// The dump is hundreds of megabytes for a program that includes <iostream> (survey P3), so nothing but one
// top-level declaration of a header, and the main file's own declarations, is held at a time: the containers
// (the translation unit, namespaces, linkage specifications) are streamed element by element.
// Clang prints locations as deltas (file and line only when they change); the filter follows them in document
// order through every node, kept or dropped, and writes them absolute (every location of the output has
// "file" and "line"). The output is Clojure-reader data (-format json: JSON Lines), one form a line: a header line, then one line per kept top-level declaration
// (a container is kept when it is in the main file or holds a kept declaration, with only the kept ones inside).
// Built with TamaGo's Go, CGO_ENABLED=0.
package main

import (
	"bufio"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"os"
	"os/exec"
	"strings"
)

// obj is a JSON object with its keys in document order, which the delta-encoded locations need.
type obj struct {
	keys []string
	vals []any
}

func (o *obj) get(k string) (any, bool) {
	for i, kk := range o.keys {
		if kk == k {
			return o.vals[i], true
		}
	}
	return nil, false
}

func (o *obj) set(k string, v any) {
	for i, kk := range o.keys {
		if kk == k {
			o.vals[i] = v
			return
		}
	}
	o.keys = append(o.keys, k)
	o.vals = append(o.vals, v)
}

func (o *obj) str(k string) string {
	v, _ := o.get(k)
	s, _ := v.(string)
	return s
}

// parseValue reads one value from the decoder, its first token already read.
func parseValue(dec *json.Decoder, tok json.Token) (any, error) {
	d, ok := tok.(json.Delim)
	if !ok {
		return tok, nil // string, json.Number, bool, nil
	}
	if d == '[' {
		arr := []any{}
		for dec.More() {
			t, err := dec.Token()
			if err != nil {
				return nil, err
			}
			v, err := parseValue(dec, t)
			if err != nil {
				return nil, err
			}
			arr = append(arr, v)
		}
		_, err := dec.Token()
		return arr, err
	}
	o := &obj{}
	for dec.More() {
		kt, err := dec.Token()
		if err != nil {
			return nil, err
		}
		t, err := dec.Token()
		if err != nil {
			return nil, err
		}
		v, err := parseValue(dec, t)
		if err != nil {
			return nil, err
		}
		o.keys = append(o.keys, kt.(string))
		o.vals = append(o.vals, v)
	}
	_, err := dec.Token()
	return o, err
}

// state is the position clang's delta encoding is relative to.
type state struct {
	file string
	line json.Number
}

// fill makes a location absolute and moves the state along, in document order; it reports the file of the
// location it ends at (the expansion's, for a macro location).
func (s *state) fill(v any) string {
	o, ok := v.(*obj)
	if !ok {
		return ""
	}
	if _, ok := o.get("offset"); !ok {
		last := ""
		for i, k := range o.keys { // spellingLoc, expansionLoc
			if k == "spellingLoc" || k == "expansionLoc" {
				last = s.fill(o.vals[i])
			}
		}
		return last
	}
	if f, ok := o.get("file"); ok {
		s.file, _ = f.(string)
	}
	if l, ok := o.get("line"); ok {
		s.line, _ = l.(json.Number)
	}
	o.set("file", s.file)
	if s.line != "" {
		o.set("line", s.line)
	}
	return s.file
}

// track walks a finished node in document order and fills its locations; it returns the file of the
// node's own "loc" ("" when it has none).
func (s *state) track(n *obj) string {
	own := ""
	for i, k := range n.keys {
		switch k {
		case "loc":
			own = s.fill(n.vals[i])
		case "range":
			if r, ok := n.vals[i].(*obj); ok {
				for j := range r.keys {
					s.fill(r.vals[j])
				}
			}
		case "inner":
			if arr, ok := n.vals[i].([]any); ok {
				for _, c := range arr {
					if co, ok := c.(*obj); ok {
						s.track(co)
					}
				}
			}
		default:
			// other keys hold nodes too (referencedDecl is only an id and a type; a few kinds hold
			// "decl" or "array_filler" nodes): walk them for locations
			s.trackAny(n.vals[i])
		}
	}
	return own
}

func (s *state) trackAny(v any) {
	switch x := v.(type) {
	case *obj:
		if _, ok := x.get("kind"); ok {
			s.track(x)
		}
	case []any:
		for _, e := range x {
			s.trackAny(e)
		}
	}
}

var containers = map[string]bool{"TranslationUnitDecl": true, "NamespaceDecl": true, "LinkageSpecDecl": true, "ExportDecl": true}

type filter struct {
	main     string
	implicit bool
	st       state
	kept     int64
	dropped  int64
}

func (f *filter) inMain(file string) bool { return file == f.main }

func (f *filter) isImplicit(n *obj) bool {
	v, _ := n.get("isImplicit")
	b, _ := v.(bool)
	return b
}

// node reads one node object (its "{" already read). A container's inner array is streamed; emit, when not
// nil, is called with each kept child of the translation unit instead of collecting it.
func (f *filter) node(dec *json.Decoder, emit func(*obj)) (n *obj, keep bool, err error) {
	n = &obj{}
	own := ""
	stream := false
	for dec.More() {
		kt, err := dec.Token()
		if err != nil {
			return nil, false, err
		}
		k := kt.(string)
		if k == "inner" && containers[n.str("kind")] {
			stream = true
			t, err := dec.Token()
			if err != nil {
				return nil, false, err
			}
			if d, _ := t.(json.Delim); d != '[' {
				return nil, false, fmt.Errorf("inner of a container is not an array")
			}
			kids := []any{}
			for dec.More() {
				t, err := dec.Token()
				if err != nil {
					return nil, false, err
				}
				if d, _ := t.(json.Delim); d != '{' {
					return nil, false, fmt.Errorf("a child is not an object")
				}
				c, ck, err := f.node(dec, nil)
				if err != nil {
					return nil, false, err
				}
				if ck {
					f.kept++
					if emit != nil {
						emit(c)
					} else {
						kids = append(kids, c)
					}
				} else {
					f.dropped++
				}
			}
			if _, err := dec.Token(); err != nil {
				return nil, false, err
			}
			if emit == nil {
				n.keys = append(n.keys, "inner")
				n.vals = append(n.vals, kids)
				if len(kids) > 0 {
					keep = true
				}
			}
			continue
		}
		t, err := dec.Token()
		if err != nil {
			return nil, false, err
		}
		v, err := parseValue(dec, t)
		if err != nil {
			return nil, false, err
		}
		n.keys = append(n.keys, k)
		n.vals = append(n.vals, v)
		switch k {
		case "loc":
			own = f.st.fill(v)
		case "range":
			if r, ok := v.(*obj); ok {
				for j := range r.keys {
					f.st.fill(r.vals[j])
				}
			}
		case "inner":
			// a non-container's children, parsed whole
			if arr, ok := v.([]any); ok {
				for _, c := range arr {
					if co, ok := c.(*obj); ok {
						f.st.track(co)
					}
				}
			}
		default:
			f.st.trackAny(v)
		}
	}
	if _, err := dec.Token(); err != nil {
		return nil, false, err
	}
	_ = stream
	if f.inMain(own) && (f.implicit || !f.isImplicit(n)) {
		keep = true
	}
	return n, keep, nil
}

func write(w io.Writer, v any) {
	switch x := v.(type) {
	case *obj:
		io.WriteString(w, "{")
		for i, k := range x.keys {
			if i > 0 {
				io.WriteString(w, ",")
			}
			kb, _ := json.Marshal(k)
			w.Write(kb)
			io.WriteString(w, ":")
			write(w, x.vals[i])
		}
		io.WriteString(w, "}")
	case []any:
		io.WriteString(w, "[")
		for i, e := range x {
			if i > 0 {
				io.WriteString(w, ",")
			}
			write(w, e)
		}
		io.WriteString(w, "]")
	default:
		b, _ := json.Marshal(x)
		w.Write(b)
	}
}

// writeEdn writes the tree as Clojure-reader data: maps with keyword keys in document order, vectors, strings
// escaped as the Clojure reader reads them (Arbace has no JSON reader, so this is the format the converter takes).
func writeEdn(w io.Writer, v any) {
	switch x := v.(type) {
	case *obj:
		io.WriteString(w, "{")
		for i, k := range x.keys {
			if i > 0 {
				io.WriteString(w, " ")
			}
			io.WriteString(w, ":")
			io.WriteString(w, ednKey(k))
			io.WriteString(w, " ")
			writeEdn(w, x.vals[i])
		}
		io.WriteString(w, "}")
	case []any:
		io.WriteString(w, "[")
		for i, e := range x {
			if i > 0 {
				io.WriteString(w, " ")
			}
			writeEdn(w, e)
		}
		io.WriteString(w, "]")
	case string:
		io.WriteString(w, ednString(x))
	case json.Number:
		io.WriteString(w, x.String())
	case bool:
		if x {
			io.WriteString(w, "true")
		} else {
			io.WriteString(w, "false")
		}
	case nil:
		io.WriteString(w, "nil")
	default:
		fmt.Fprintf(w, "%v", x)
	}
}

// ednKey is a JSON key as a keyword's name; clang's keys are plain words, anything else is written as a string
// keyword the reader accepts after the colon.
func ednKey(k string) string {
	for _, r := range k {
		if !(r >= 'a' && r <= 'z' || r >= 'A' && r <= 'Z' || r >= '0' && r <= '9' || r == '_' || r == '-') {
			return "_" + fmt.Sprintf("%x", k)
		}
	}
	return k
}

func ednString(s string) string {
	var b strings.Builder
	b.WriteByte('"')
	for _, r := range s {
		switch r {
		case '"':
			b.WriteString(`\"`)
		case '\\':
			b.WriteString(`\\`)
		case '\n':
			b.WriteString(`\n`)
		case '\t':
			b.WriteString(`\t`)
		case '\r':
			b.WriteString(`\r`)
		default:
			if r < 0x20 || r == 0x7f {
				fmt.Fprintf(&b, `\u%04x`, r)
			} else {
				b.WriteRune(r)
			}
		}
	}
	b.WriteByte('"')
	return b.String()
}

func run(in io.Reader, out io.Writer, main string, implicit bool, edn bool) (kept, dropped int64, err error) {
	dec := json.NewDecoder(bufio.NewReaderSize(in, 1<<20))
	dec.UseNumber()
	t, err := dec.Token()
	if err != nil {
		return 0, 0, err
	}
	if d, _ := t.(json.Delim); d != '{' {
		return 0, 0, fmt.Errorf("the dump does not start with an object")
	}
	f := &filter{main: main, implicit: implicit}
	bw := bufio.NewWriterSize(out, 1<<20)
	if edn {
		fmt.Fprintf(bw, "{:cxxdump 1 :main %s}\n", ednString(main))
	} else {
		fmt.Fprintf(bw, "{\"cxxdump\":1,\"main\":%q}\n", main)
	}
	emit := func(c *obj) {
		if edn {
			writeEdn(bw, c)
		} else {
			write(bw, c)
		}
		bw.WriteString("\n")
	}
	if _, _, err := f.node(dec, emit); err != nil {
		return f.kept, f.dropped, err
	}
	return f.kept, f.dropped, bw.Flush()
}

func main() {
	mainFile := flag.String("main", "", "the main file as clang prints it (default: the last argument)")
	implicit := flag.Bool("implicit", true, "keep implicit declarations of the main file")
	stdin := flag.Bool("stdin", false, "read a dump from the standard input instead of running clang")
	outName := flag.String("o", "", "output file (default standard output)")
	format := flag.String("format", "edn", "output format: edn (Clojure-reader data, one form a line) or json (JSON Lines)")
	clang := flag.String("clang", "clang++", "the clang driver")
	flag.Parse()
	args := flag.Args()
	out := io.Writer(os.Stdout)
	if *outName != "" {
		fh, err := os.Create(*outName)
		if err != nil {
			fmt.Fprintln(os.Stderr, "cxxdump:", err)
			os.Exit(1)
		}
		defer fh.Close()
		out = fh
	}
	if *mainFile == "" && len(args) > 0 {
		*mainFile = args[len(args)-1]
	}
	var in io.Reader = os.Stdin
	var cmd *exec.Cmd
	if !*stdin {
		if len(args) == 0 {
			fmt.Fprintln(os.Stderr, "usage: cxxdump [-main NAME] [-implicit=false] -- CLANG-ARGS... FILE")
			os.Exit(2)
		}
		full := append([]string{"-Xclang", "-ast-dump=json", "-fsyntax-only"}, args...)
		cmd = exec.Command(*clang, full...)
		cmd.Stderr = os.Stderr
		p, err := cmd.StdoutPipe()
		if err != nil {
			fmt.Fprintln(os.Stderr, "cxxdump:", err)
			os.Exit(1)
		}
		if err := cmd.Start(); err != nil {
			fmt.Fprintln(os.Stderr, "cxxdump:", err)
			os.Exit(1)
		}
		in = p
	}
	kept, dropped, err := run(in, out, *mainFile, *implicit, *format == "edn")
	if cmd != nil {
		io.Copy(io.Discard, in) // let clang finish
		if werr := cmd.Wait(); werr != nil {
			// the oracle rejects the unit (its diagnostics are on the standard error): the filter's own
			// error, if any, is only the dump cut short
			err = fmt.Errorf("clang rejected the unit: %v", werr)
		}
	}
	if err != nil {
		fmt.Fprintln(os.Stderr, "cxxdump:", err)
		os.Exit(1)
	}
	fmt.Fprintf(os.Stderr, "cxxdump: kept %d, dropped %d declarations of %s\n", kept, dropped, strings.TrimSpace(*mainFile))
}
