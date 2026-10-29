// ccdump: cc's lexer and preprocessor as data, the oracle of Arbace's port of them (arbace.repr.c.lex,
// arbace.repr.c.pp; doc/C23-SURVEY.md, C1.3 and "As built: C2"). It imports go-whim's crefactor/cc
// (or, with the build tag upstream, modernc.org/cc/v4 v4.29.7) and copies none of it; bin/repr-c
// ccdump-build builds it.
//
//	ccdump -mode lex|pp -host DIR -list FILE -o DIR [-base DIR] [-skip-done]
//
// The list holds one JSON object a line: {"name": NAME, "std": STD, "pre": TEXT, "inc": [DIR...],
// "out": PATH}: NAME the file (relative to -base, which is the working directory while it runs), STD
// the standard whose predefined macros it is read with (the host's DIR/predefined/STD.h), TEXT
// lines appended to them (the file's -D and -U as #define and #undef), the DIRs its -I, and PATH
// where under -o its dump goes. The host's DIR/search.txt lists the reference's search list (gcc's
// include directory first, CS32 (a)). cc is configured with those answers alone (go-whim's SetHost):
// it never runs a compiler.
//
// A dump is one line a token, tab-separated: position (token.Position's String; "-" for a separator's
// ' ' token, whose position cc does not keep), kind (cc's name: IDENTIFIER, PPNUMBER, '(' ...), source
// text, separator (the white space and comments before it), and in mode pp the name of the
// object-like macro whose expansion made it. Texts are escaped byte for byte: \\, \t, \n, \r and any
// byte outside 0x20-0x7e as \xHH. Then "#error\tMSG" for each error, and "#panic\tMSG" when cc
// panicked (the stream up to it kept). Before each file its output path is written to DIR/.current,
// so that a driver can mark a file the helper hangs on and resume with -skip-done.
package main

import (
	"bufio"
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
)

type host struct {
	predefined string
	include    []string
	sysInclude []string
}

var hostMu sync.Mutex

type entry struct {
	Name string   `json:"name"`
	Std  string   `json:"std"`
	Pre  string   `json:"pre"`
	Inc  []string `json:"inc"`
	Out  string   `json:"out"`
}

func esc(b []byte) string {
	var w strings.Builder
	for _, c := range b {
		switch {
		case c == '\\':
			w.WriteString(`\\`)
		case c == '\t':
			w.WriteString(`\t`)
		case c == '\n':
			w.WriteString(`\n`)
		case c == '\r':
			w.WriteString(`\r`)
		case c < 0x20 || c > 0x7e:
			fmt.Fprintf(&w, `\x%02x`, c)
		default:
			w.WriteByte(c)
		}
	}
	return w.String()
}

func main() {
	mode := flag.String("mode", "pp", "lex or pp")
	hostDir := flag.String("host", "", "the recorded host data (test/c/host)")
	list := flag.String("list", "", "the files, JSON lines")
	out := flag.String("o", "", "the output directory")
	base := flag.String("base", "", "the working directory while reading")
	skipDone := flag.Bool("skip-done", false, "skip the files whose dump exists (a run resumed)")
	flag.Parse()
	if *list == "" || *out == "" || (*mode == "pp" && *hostDir == "") {
		fmt.Fprintln(os.Stderr, "usage: ccdump -mode lex|pp -host DIR -list FILE -o DIR [-base DIR]")
		os.Exit(2)
	}
	abs := func(p string) string { a, err := filepath.Abs(p); check(err); return a }
	*list, *out = abs(*list), abs(*out)
	check(os.MkdirAll(*out, 0o755))
	var sys []string
	predef := map[string]string{}
	if *hostDir != "" {
		*hostDir = abs(*hostDir)
		b, err := os.ReadFile(filepath.Join(*hostDir, "search.txt"))
		check(err)
		for _, l := range strings.Split(strings.TrimSpace(string(b)), "\n") {
			sys = append(sys, l)
		}
	}
	lb, err := os.ReadFile(*list)
	check(err)
	if *base != "" {
		check(os.Chdir(*base))
	}
	n := 0
	for _, l := range bytes.Split(lb, []byte("\n")) {
		if len(bytes.TrimSpace(l)) == 0 {
			continue
		}
		var e entry
		check(json.Unmarshal(l, &e))
		o := filepath.Join(*out, e.Out)
		if *skipDone {
			if _, err := os.Stat(o); err == nil {
				continue
			}
		}
		check(os.WriteFile(filepath.Join(*out, ".current"), []byte(e.Out), 0o644))
		var buf bytes.Buffer
		w := bufio.NewWriter(&buf)
		switch *mode {
		case "lex":
			dumpLex(w, e)
		case "pp":
			p, ok := predef[e.Std]
			if !ok {
				b, err := os.ReadFile(filepath.Join(*hostDir, "predefined", e.Std+".h"))
				check(err)
				p = string(b)
				predef[e.Std] = p
			}
			dumpPP(w, e, &host{predefined: p + e.Pre, include: e.Inc, sysInclude: sys})
		default:
			fmt.Fprintf(os.Stderr, "ccdump: mode lex or pp, not %s\n", *mode)
			os.Exit(2)
		}
		check(w.Flush())
		check(os.MkdirAll(filepath.Dir(o), 0o755))
		check(os.WriteFile(o, buf.Bytes(), 0o644))
		n++
	}
	fmt.Printf("ccdump (%s cc): %d files\n", flavor, n)
}

func check(err error) {
	if err != nil {
		fmt.Fprintln(os.Stderr, "ccdump:", err)
		os.Exit(1)
	}
}

func pos(t *Token) string {
	if t.Ch == ' ' {
		return "-"
	}
	return t.Position().String()
}

func dumpLex(w *bufio.Writer, e entry) {
	src, err := os.ReadFile(e.Name)
	if err != nil {
		fmt.Fprintf(w, "#error\t%s\n", esc([]byte(err.Error())))
		return
	}
	errs, err := arbaceLex(e.Name, src, func(t Token) {
		fmt.Fprintf(w, "%s\t%s\t%s\t%s\n", pos(&t), t.Name(), esc(t.Src()), esc(t.Sep()))
	})
	if err != nil {
		errs = append(errs, err.Error())
	}
	for _, m := range errs {
		fmt.Fprintf(w, "#error\t%s\n", esc([]byte(m)))
	}
}

func dumpPP(w *bufio.Writer, e entry, h *host) {
	defer func() {
		if r := recover(); r != nil {
			fmt.Fprintf(w, "#panic\t%s\n", esc([]byte(fmt.Sprint(r))))
		}
	}()
	cfg, err := newConfig(h)
	if err != nil {
		fmt.Fprintf(w, "#error\t%s\n", esc([]byte(err.Error())))
		return
	}
	srcs := []Source{{Name: "<predefined>", Value: cfg.Predefined}, {Name: e.Name}}
	errs, err := arbacePreprocess(cfg, srcs, func(t Token, m string) {
		fmt.Fprintf(w, "%s\t%s\t%s\t%s\t%s\n", pos(&t), t.Name(), esc(t.Src()), esc(t.Sep()), m)
	})
	if err != nil {
		errs = append(errs, err.Error())
	}
	for _, m := range errs {
		fmt.Fprintf(w, "#error\t%s\n", esc([]byte(m)))
	}
}
