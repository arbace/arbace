// Command goplan prints, as EDN, what the box's build driver (arbace.box.build,
// doc/go/BOX-NOTES.md, S7) needs of a program's packages: go list -deps
// -json's fields, in its order (each package after those it imports), for
// the GOOS and GOARCH of its environment, run with the go of GOROOT in the
// current directory's module.
//
//	goplan PKG
//
// It prints
//
//	{:goos "tamago" :goarch "amd64" :goamd64 "v1" :goarm64 "v8.0" :main "PKG"
//	 :packages [{:path P :name NAME :dir DIR :std BOOL :lang "go1.27" :go [FILE ...]
//	             :s [FILE ...] :imports [P ...] :importmap {FROM TO}} ...]}
//
// leaving out unsafe (which nothing compiles), and refuses packages with cgo,
// .syso or other kinds of source the driver does not build. A package that
// embeds files has :embed-patterns [PATTERN ...] and :embed-files [FILE ...]
// too (go list's EmbedPatterns and EmbedFiles), which S8's planner
// (arbace.box.plan) is checked against; the driver builds a package that
// embeds only from that planner's :embed.
package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strconv"
)

type pkg struct {
	ImportPath, Dir, Name string
	Standard              bool
	GoFiles, SFiles       []string
	CgoFiles, SysoFiles   []string
	CFiles, CXXFiles      []string
	EmbedPatterns         []string
	EmbedFiles            []string
	Imports               []string
	ImportMap             map[string]string
	Module                *struct{ GoVersion string }
	Error                 *struct{ Err string }
}

func q(s string) string { return strconv.Quote(s) }

func strs(ss []string) string {
	var b bytes.Buffer
	b.WriteByte('[')
	for i, s := range ss {
		if i > 0 {
			b.WriteByte(' ')
		}
		b.WriteString(q(s))
	}
	b.WriteByte(']')
	return b.String()
}

func fail(f string, a ...any) {
	fmt.Fprintf(os.Stderr, "goplan: "+f+"\n", a...)
	os.Exit(1)
}

func main() {
	if len(os.Args) != 2 {
		fail("usage: goplan PKG")
	}
	goroot := os.Getenv("GOROOT")
	if goroot == "" {
		fail("no GOROOT")
	}
	cmd := exec.Command(filepath.Join(goroot, "bin", "go"), "list", "-deps", "-json", os.Args[1])
	cmd.Stderr = os.Stderr
	out, err := cmd.Output()
	if err != nil {
		fail("go list: %v", err)
	}
	stdLang := "go1.27"
	if b, err := os.ReadFile(filepath.Join(goroot, "src", "go.mod")); err == nil {
		for _, l := range bytes.Split(b, []byte("\n")) {
			if f := bytes.Fields(l); len(f) == 2 && string(f[0]) == "go" {
				stdLang = "go" + string(f[1])
			}
		}
	}
	env := func(k, def string) string {
		if v := os.Getenv(k); v != "" {
			return v
		}
		return def
	}
	fmt.Printf("{:goos %s :goarch %s :goamd64 %s :goarm64 %s :main %s\n :packages [", q(env("GOOS", "tamago")), q(env("GOARCH", "amd64")),
		q(env("GOAMD64", "v1")), q(env("GOARM64", "v8.0")), q(os.Args[1]))
	dec := json.NewDecoder(bytes.NewReader(out))
	first := true
	for {
		var p pkg
		if err := dec.Decode(&p); err == io.EOF {
			break
		} else if err != nil {
			fail("go list's output: %v", err)
		}
		if p.Error != nil {
			fail("%s: %s", p.ImportPath, p.Error.Err)
		}
		if p.ImportPath == "unsafe" {
			continue
		}
		if len(p.CgoFiles)+len(p.SysoFiles)+len(p.CFiles)+len(p.CXXFiles) > 0 {
			fail("%s: cgo, .syso or C files: not built by the box's driver", p.ImportPath)
		}
		lang := stdLang
		if p.Module != nil && p.Module.GoVersion != "" {
			lang = "go" + p.Module.GoVersion
		}
		var imports []string
		for _, i := range p.Imports {
			if i != "unsafe" {
				imports = append(imports, i)
			}
		}
		var froms []string
		for k := range p.ImportMap {
			froms = append(froms, k)
		}
		sort.Strings(froms)
		var im bytes.Buffer
		for i, k := range froms {
			if i > 0 {
				im.WriteByte(' ')
			}
			im.WriteString(q(k) + " " + q(p.ImportMap[k]))
		}
		if !first {
			fmt.Print("\n            ")
		}
		first = false
		embed := ""
		if len(p.EmbedFiles) > 0 {
			embed = "\n             :embed-patterns " + strs(p.EmbedPatterns) + "\n             :embed-files " + strs(p.EmbedFiles)
		}
		fmt.Printf("{:path %s :name %s :dir %s :std %t :lang %s\n             :go %s\n             :s %s\n             :imports %s\n             :importmap {%s}%s}",
			q(p.ImportPath), q(p.Name), q(p.Dir), p.Standard, q(lang), strs(p.GoFiles), strs(p.SFiles), strs(imports), im.String(), embed)
	}
	fmt.Println("]}")
}
