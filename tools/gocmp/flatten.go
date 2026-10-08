package main

// The canonical layout, for the export and code levels.
//
// gc's generated code depends on source positions, not only through the
// position tables and DWARF: the instruction scheduler orders otherwise
// unordered values by position ("favor in-order line stepping",
// ssa/schedule.go), inline marks become NOPs when no instruction of the same
// line can carry them, and frame slot merging orders same-named variables by
// position (liveness/mergelocals.go). A printer that does not reproduce the
// original layout therefore changes the code of some functions without
// changing the program.
//
// So both sides are compiled from a canonical layout of their tokens, which
// two spellings of a tree that the tree level identifies share:
//
//   - every top-level function and every spec (inside a group or not) starts
//     with //line FILE:1, so lines count from the start of the declaration
//     (FILE is the original file's path, on both sides);
//   - every line starts with 256 spaces, past which gc's position encoding
//     saturates the column (cmd/internal/src, colMax 255): a position is a
//     line alone;
//   - a new line starts after every ; and {, except inside the braces of a
//     struct or interface type, unless the next token is ; ) or } (then not at
//     all: a newline after ) or } could end a statement) or a top-level
//     import, var, const, type or func;
//     (so statements, case clauses, struct fields and composite literal
//     elements count, but not explicit semicolons, empty statements, or the
//     ; before a closing bracket);
//   - directives (//go:..., // +build) stand on their own lines;
//   - automatic semicolons become explicit, other comments are dropped, and
//     raw strings become interpreted ones (their newlines would count).
//
// Parentheses, trailing commas, literal spellings, elided composite literal
// types, import order and grouping, the grouping of declarations, of fields
// and of parameters then leave every position as it is, and every physical
// line too (layouts that agreed on the lines of //line directives but not on
// physical lines still gave closures inlined elsewhere other names).
// Positions remain distinct between statements, which gc's tie-breaks need:
// with every position equal, frame slot merging was found to depend on map
// order.

import (
	"bytes"
	"go/ast"
	"go/parser"
	"go/scanner"
	"go/token"
	"strconv"
	"strings"
)

// flatten returns the canonical layout of a Go source, or the source itself
// if it does not parse.
func flatten(name string, src []byte) []byte {
	fset := token.NewFileSet()
	f, err := parser.ParseFile(fset, name, src, parser.SkipObjectResolution)
	if err != nil {
		return src
	}
	starts := map[int]bool{} // offsets of the tokens that start a declaration
	for _, d := range f.Decls {
		switch d := d.(type) {
		case *ast.FuncDecl:
			starts[fset.Position(d.Pos()).Offset] = true
		case *ast.GenDecl:
			for _, s := range d.Specs {
				starts[fset.Position(s.Pos()).Offset] = true
			}
		}
	}
	file := fset.AddFile(name, -1, len(src))
	var s scanner.Scanner
	s.Init(file, src, nil, scanner.ScanComments)
	pad := strings.Repeat(" ", 256)
	line := "\n//line " + name + ":1\n" + pad
	var b bytes.Buffer
	b.Grow(len(src))
	b.WriteString(pad)
	seenPackage := false
	newline := false  // start a new line before the next token
	depth := 0        // of (, [ and {
	var braces []bool // per open {: whether it opens a struct or interface type
	prev := token.ILLEGAL
	for {
		pos, tok, lit := s.Scan()
		switch {
		case tok == token.EOF:
			b.WriteString("\n")
			return b.Bytes()
		case tok == token.COMMENT:
			text := strings.TrimRight(lit, " \t\r")
			if !isDirective(text) {
				continue
			}
			b.WriteString("\n" + text + "\n")
			if !seenPackage {
				b.WriteString("\n") // a build constraint ends with a blank line
			}
			b.WriteString(pad)
			newline = false
			continue
		case tok == token.SEMICOLON:
			lit = ";"
		case tok == token.STRING && strings.HasPrefix(lit, "`"):
			lit = strconv.Quote(strings.ReplaceAll(lit[1:len(lit)-1], "\r", ""))
		case tok == token.PACKAGE:
			seenPackage = true
		}
		declKeyword := depth == 0 && (tok == token.IMPORT || tok == token.VAR || tok == token.CONST || tok == token.TYPE || tok == token.FUNC)
		if starts[file.Offset(pos)] {
			b.WriteString(line)
			newline = false
		} else if newline {
			if tok != token.SEMICOLON && tok != token.RPAREN && tok != token.RBRACE && !declKeyword {
				b.WriteString("\n" + pad)
			}
			newline = false
		}
		if lit == "" {
			lit = tok.String()
		}
		b.WriteString(" " + lit)
		inType := len(braces) > 0 && braces[len(braces)-1]
		switch tok {
		case token.LPAREN, token.LBRACK:
			depth++
		case token.RPAREN, token.RBRACK:
			depth--
		case token.LBRACE:
			depth++
			braces = append(braces, prev == token.STRUCT || prev == token.INTERFACE)
			newline = !braces[len(braces)-1]
		case token.RBRACE:
			depth--
			if len(braces) > 0 {
				braces = braces[:len(braces)-1]
			}
		case token.SEMICOLON:
			newline = !inType
		}
		prev = tok
	}
}
