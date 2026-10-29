package main

// The tree mode (doc/C23-SURVEY.md, C1.3 and "As built: C3"): cc's Parse over a file, its tree written
// by a reflection walk, the oracle of arbace.repr.c.parse (whose arbace.repr.c.tree/dump writes its
// trees in the same format).
//
// A tree is one line a node or token, pre-order, each line beginning with its depth (the root's
// line, "0\tTranslationUnit\tTranslationUnit", at 0; a number, not an indentation, since cc's lists
// nest): a node's line is "DEPTH\tFIELD\tTYPE" or "DEPTH\tFIELD\tTYPE\tCASE" (the field of its
// parent that holds it, its Go type's name, its Case's name when it has one), a token's line
// "DEPTH\tFIELD\t@POSITION\tKIND\tSRC\tSEP" (token.Position's String, cc's name of its kind, its source text
// and separator escaped as in the other modes). Fields are walked in the struct's order; an absent
// child (a nil pointer or interface) and an absent token (one of no source) are not written; the
// elements of a slice are written as FIELD[I]. Then "#error\tMSG" for each of the parser's errors
// (its own list: cc drops an error on the line of the one before), "#notree" when Parse gave no
// tree, and "#panic\tMSG" when cc panicked (CCDUMP_STACK set: its stack on the standard error).

import (
	"bufio"
	"fmt"
	"os"
	"reflect"
	"runtime/debug"
)

var tokenType = reflect.TypeOf(Token{})

func dumpTree(w *bufio.Writer, e entry, h *host) {
	defer func() {
		if r := recover(); r != nil {
			fmt.Fprintf(w, "#panic\t%s\n", esc([]byte(fmt.Sprint(r))))
			if os.Getenv("CCDUMP_STACK") != "" {
				os.Stderr.Write(debug.Stack())
			}
		}
	}()
	cfg, err := newConfig(h)
	if err != nil {
		fmt.Fprintf(w, "#error\t%s\n", esc([]byte(err.Error())))
		return
	}
	srcs := []Source{{Name: "<predefined>", Value: cfg.Predefined}, {Name: "<builtin>", Value: builtin}, {Name: e.Name}}
	ast, errs, err := arbaceParse(cfg, srcs)
	if err != nil {
		errs = append(errs, err.Error())
	}
	if ast != nil && ast.TranslationUnit != nil {
		walk(w, 0, "TranslationUnit", reflect.ValueOf(ast.TranslationUnit))
	}
	for _, m := range errs {
		fmt.Fprintf(w, "#error\t%s\n", esc([]byte(m)))
	}
	if ast == nil {
		fmt.Fprintf(w, "#notree\n")
	}
}

func walk(w *bufio.Writer, depth int, field string, v reflect.Value) {
	for v.Kind() == reflect.Interface {
		if v.IsNil() {
			return
		}
		v = v.Elem()
	}
	if v.Kind() != reflect.Pointer || v.IsNil() {
		return
	}
	s := v.Elem()
	t := s.Type()
	ind := fmt.Sprintf("%d\t", depth)
	if c := s.FieldByName("Case"); c.IsValid() {
		fmt.Fprintf(w, "%s%s\t%s\t%v\n", ind, field, t.Name(), c.Interface())
	} else {
		fmt.Fprintf(w, "%s%s\t%s\n", ind, field, t.Name())
	}
	for i := 0; i < t.NumField(); i++ {
		f := t.Field(i)
		if !f.IsExported() || f.Name == "Case" {
			continue
		}
		fv := s.Field(i)
		switch {
		case f.Type == tokenType:
			tok(w, depth+1, f.Name, fv.Interface().(Token))
		case fv.Kind() == reflect.Slice:
			for j := 0; j < fv.Len(); j++ {
				nm := fmt.Sprintf("%s[%d]", f.Name, j)
				if x := fv.Index(j); x.Type() == tokenType {
					tok(w, depth+1, nm, x.Interface().(Token))
				} else {
					walk(w, depth+1, nm, x)
				}
			}
		default:
			walk(w, depth+1, f.Name, fv)
		}
	}
}

func tok(w *bufio.Writer, depth int, field string, t Token) {
	if t.Src() == nil {
		return
	}
	fmt.Fprintf(w, "%d\t%s\t@%s\t%s\t%s\t%s\n", depth, field, t.Position(), t.Name(), esc(t.Src()), esc(t.Sep()))
}
