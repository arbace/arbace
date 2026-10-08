package main

// gocmp mutate: seeded mutations of a package, to show that each level of the
// oracle catches the changes it should. One site of the package's files for
// the configuration is changed, chosen deterministically from the seed:
//
//	swap   the operands of a binary expression: x op y -> (y) op (x)
//	const  an integer literal: n -> n+1
//	drop   a statement (a call, an assignment with =, an x++ or x--)

import (
	"flag"
	"fmt"
	"go/ast"
	"go/constant"
	"go/token"
	"hash/fnv"
	"os"
	"path/filepath"
	"sort"
	"strings"
)

type site struct {
	file       string // base name
	start, end int    // byte offsets of the replaced text
	repl       string
	desc       string
	line       int
}

func mainMutate(args []string) int {
	fs := flag.NewFlagSet("gocmp mutate", flag.ExitOnError)
	fs.StringVar(&goCmd, "go", defaultGo(), "the go command")
	op := fs.String("op", "swap", "mutation: swap, const or drop")
	seed := fs.Int("seed", 1, "seed choosing the site")
	fs.Usage = usage(fs, "usage: gocmp mutate -op swap|const|drop [-seed N] A OUTDIR\n\nWrites A's files for the configuration to OUTDIR (a file, if A is one), one of them\nmutated. Exit status: 0 mutated, 2 error, 4 no site for the mutation.\n")
	fs.Parse(args)
	if fs.NArg() != 2 {
		fs.Usage()
		return 2
	}
	p, err := listPackage(fs.Arg(0))
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		return 2
	}
	sort.Strings(p.files)
	srcs := map[string][]byte{}
	var sites []site
	for _, name := range p.files {
		sf, err := parseFile(filepath.Join(p.dir, name))
		if err != nil {
			fmt.Fprintln(os.Stderr, err)
			return 2
		}
		srcs[name] = sf.src
		sites = append(sites, findSites(sf, name, *op)...)
	}
	if len(sites) == 0 {
		fmt.Println("no site")
		return 4
	}
	h := fnv.New64a()
	fmt.Fprintf(h, "%d %s %v", *seed, *op, p.files)
	s := sites[h.Sum64()%uint64(len(sites))]
	out := fs.Arg(1)
	if p.pattern[0] == "." {
		if err := os.MkdirAll(out, 0o755); err != nil {
			fmt.Fprintln(os.Stderr, err)
			return 2
		}
	}
	for _, name := range p.files {
		b := srcs[name]
		if name == s.file {
			b = append(append(append([]byte{}, b[:s.start]...), s.repl...), b[s.end:]...)
		}
		path := out
		if p.pattern[0] == "." {
			path = filepath.Join(out, name)
		} else if len(p.files) > 1 {
			// a program of several files: OUTPUT is the first file's path, the
			// others go next to it
			path = filepath.Join(filepath.Dir(out), name)
		}
		if err := os.WriteFile(path, b, 0o644); err != nil {
			fmt.Fprintln(os.Stderr, err)
			return 2
		}
	}
	fmt.Printf("mutated %s:%d: %s\n", s.file, s.line, s.desc)
	return 0
}

func findSites(sf *srcFile, name, op string) []site {
	off := func(p token.Pos) int { return sf.fset.PositionFor(p, false).Offset }
	text := func(n ast.Node) string { return string(sf.src[off(n.Pos()):off(n.End())]) }
	var sites []site
	add := func(n ast.Node, repl, desc string) {
		sites = append(sites, site{file: name, start: off(n.Pos()), end: off(n.End()), repl: repl, desc: desc, line: sf.fset.PositionFor(n.Pos(), false).Line})
	}
	ast.Inspect(sf.f, func(n ast.Node) bool {
		switch n := n.(type) {
		case *ast.BinaryExpr:
			if op == "swap" {
				x, y := text(n.X), text(n.Y)
				if !sameExpr(n.X, n.Y) { // else the mutant is the same program
					add(n, "("+y+") "+n.Op.String()+" ("+x+")", fmt.Sprintf("swap %s %s %s", short(x), n.Op, short(y)))
				}
			}
		case *ast.BasicLit:
			if op == "const" && n.Kind == token.INT {
				v := constant.MakeFromLiteral(n.Value, n.Kind, 0)
				if v.Kind() == constant.Int {
					w := constant.BinaryOp(v, token.ADD, constant.MakeInt64(1))
					add(n, w.ExactString(), fmt.Sprintf("const %s -> %s", n.Value, w.ExactString()))
				}
			}
		case *ast.BlockStmt:
			dropSites(n.List, op, add, text)
		case *ast.CaseClause:
			dropSites(n.Body, op, add, text)
		case *ast.CommClause:
			dropSites(n.Body, op, add, text)
		}
		return true
	})
	return sites
}

// dropSites adds the statements of a statement list that can be dropped.
func dropSites(list []ast.Stmt, op string, add func(ast.Node, string, string), text func(ast.Node) string) {
	if op != "drop" {
		return
	}
	for _, s := range list {
		switch s := s.(type) {
		case *ast.ExprStmt, *ast.IncDecStmt:
			add(s, "", "drop "+short(text(s)))
		case *ast.AssignStmt:
			if s.Tok != token.DEFINE {
				add(s, "", "drop "+short(text(s)))
			}
		}
	}
}

func short(s string) string {
	s = strings.Join(strings.Fields(s), " ")
	if len(s) > 60 {
		return s[:57] + "..."
	}
	return s
}
