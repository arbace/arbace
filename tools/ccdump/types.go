//go:build !upstream

package main

// The types mode (doc/C23-SURVEY.md, "As built: C4"): cc's Translate over a file (its checker), the
// checker's decisions on the main file's external declarations written as annotations, the oracle of
// arbace.c.check (bin/ci annotations; arbace.c.annot writes the checker's in the same format).
//
// One block a line of the main file holding external declarations: "@LINE", then one line an
// annotation, in the tree's pre-order:
//
//	d NAME LINKAGE TYPE         a declarator (LINKAGE x, i or n: external, internal, none)
//	e TYPE [= VALUE]            an expression (not a parenthesized one, not one level passing another
//	                            through), its type decayed (an array or function type is written as
//	                            the pointer it decays to), VALUE its integer value when constant
//	s TYPE SIZE ALIGN           a struct or union defined (its specifier with a member list)
//	m STRUCT NAME OFFSET - TYPE  its members; a bit-field "m STRUCT NAME @BITPOS BITS TYPE", its bit position
//	                            in the record and its width (a zero-width one not written)
//	n NAME VALUE                an enumerator
//
// TYPE is written without qualifiers (cc's types do not keep them): the base types by their C names
// (int, unsigned, long, unsigned long, char, signed char, _Bool, long double, __int128, ...), *T, [N]T
// ([]T incomplete, [*]T a VLA), fn(T,T,...)R (fn()R: no parameter list, fn(void)R), struct TAG, union
// TAG, enum TAG (an anonymous one #anon), ? an invalid type. Then "#error\tMSG" for each error of
// Translate (cc's checker gives no tree then: "#notree"), "#panic\tMSG" when cc panicked.

import (
	"bufio"
	"fmt"
	"os"
	"reflect"
	"runtime/debug"
	"strings"

	cc "github.com/arbace/go-whim/crefactor/cc"
)

func dumpTypes(w *bufio.Writer, e entry, h *host) {
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
	ast, err := cc.Translate(cfg, srcs)
	if err != nil {
		for _, m := range strings.Split(err.Error(), "\n") {
			if m != "" {
				fmt.Fprintf(w, "#error\t%s\n", esc([]byte(m)))
			}
		}
		fmt.Fprintf(w, "#notree\n")
		return
	}
	line := -1
	for l := ast.TranslationUnit; l != nil; l = l.TranslationUnit {
		ed := l.ExternalDeclaration
		p := ed.Position()
		fl := firstLine(reflect.ValueOf(ed), e.Name)
		if fl == 0 {
			continue // no token of the main file
		}
		p.Line = fl // the item's first token's raw line (cc's Position skips leading attributes and applies #line)
		if p.Line != line {
			line = p.Line
			fmt.Fprintf(w, "@%d\n", line)
		}
		annot(w, reflect.ValueOf(ed), false, false)
	}
}

// the levels that pass their operand through: no annotation of their own
var passCases = map[string]bool{
	"AssignmentExpressionCond": true, "ConditionalExpressionLOr": true, "LogicalOrExpressionLAnd": true,
	"LogicalAndExpressionOr": true, "InclusiveOrExpressionXor": true, "ExclusiveOrExpressionAnd": true,
	"AndExpressionEq": true, "EqualityExpressionRel": true, "RelationalExpressionShift": true,
	"ShiftExpressionAdd": true, "AdditiveExpressionMul": true, "MultiplicativeExpressionCast": true,
	"CastExpressionUnary": true, "UnaryExpressionPostfix": true, "PostfixExpressionPrimary": true,
	"PrimaryExpressionExpr": true,
}

type typed interface {
	Type() cc.Type
	Value() cc.Value
}

func annot(w *bufio.Writer, v reflect.Value, member, inDecl bool) {
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
	cs := ""
	if c := s.FieldByName("Case"); c.IsValid() {
		cs = fmt.Sprint(c.Interface())
	}
	switch x := v.Interface().(type) {
	case *cc.InitDeclarator:
		if x.Declarator != nil && x.Declarator.Name() == "__func__" {
			return // cc's injected __func__ (the forms have none)
		}
	case *cc.StructDeclarationList:
		member = true
	case *cc.ParameterDeclaration:
		member, inDecl = false, false
	case *cc.Declarator:
		if member || inDecl {
			inDecl = true
			break // a member (its s and m lines), or a parenthesized declarator inside its declarator
		}
		inDecl = true
		lk := "x"
		switch x.Linkage() {
		case cc.Internal:
			lk = "i"
		case cc.None:
			lk = "n"
		}
		fmt.Fprintf(w, "d %s %s %s\n", x.Name(), lk, tstr(x.Type()))
	case *cc.Enumerator:
		fmt.Fprintf(w, "n %s %s\n", x.Token.SrcStr(), vstr(x.Value()))
	case *cc.StructOrUnionSpecifier:
		if x.StructDeclarationList != nil {
			ty := x.Type()
			fmt.Fprintf(w, "s %s %d %d\n", tstr(ty), ty.Size(), ty.Align())
			fields(w, ty, 0)
		}
	case *cc.ExpressionList:
		if x.ExpressionList != nil && x.Type() != nil {
			fmt.Fprintf(w, "e %s%s\n", tstr(decayed(x.Type())), valSuffix(x.Value()))
		}
		// the comma's operands
		for l := x; l != nil; l = l.ExpressionList {
			annot(w, reflect.ValueOf(l.AssignmentExpression), member, inDecl)
		}
		return
	default:
		if te, ok := v.Interface().(typed); ok && strings.HasSuffix(t.Name(), "Expression") && t.Name() != "ConstantExpression" && !passCases[cs] {
			fmt.Fprintf(w, "e %s%s\n", tstr(decayed(te.Type())), valSuffix(te.Value()))
		}
	}
	for i := 0; i < t.NumField(); i++ {
		f := t.Field(i)
		if !f.IsExported() || f.Name == "Case" || f.Type == tokenType {
			continue
		}
		fv := s.Field(i)
		if fv.Kind() == reflect.Slice {
			for j := 0; j < fv.Len(); j++ {
				if x := fv.Index(j); x.Type() != tokenType {
					annot(w, x, member, inDecl)
				}
			}
			continue
		}
		annot(w, fv, member, inDecl)
	}
}

func fields(w *bufio.Writer, ty cc.Type, base int64) {
	var n int
	var field func(int) *cc.Field
	switch x := ty.(type) {
	case *cc.StructType:
		n, field = x.NumFields(), x.FieldByIndex
	case *cc.UnionType:
		n, field = x.NumFields(), x.FieldByIndex
	default:
		return
	}
	for i := 0; i < n; i++ {
		f := field(i)
		if f == nil {
			continue
		}
		nm := f.Name()
		if nm == "" {
			nm = "#anon"
		}
		if f.IsBitfield() {
			if f.ValueBits() == 0 {
				continue // a zero-width bit-field: no place of its own
			}
			// a bit-field at its bit position in the record
			fmt.Fprintf(w, "m %s %s @%d %d %s\n", tstr(ty), nm, 8*(base+f.Offset())+int64(f.OffsetBits()), f.ValueBits(), tstr(f.Type()))
			continue
		}
		fmt.Fprintf(w, "m %s %s %d - %s\n", tstr(ty), nm, base+f.Offset(), tstr(f.Type()))
	}
}

func decayed(t cc.Type) cc.Type {
	if t == nil {
		return t
	}
	switch t.Kind() {
	case cc.Array, cc.Function:
		return t.Decay()
	}
	return t
}

func valSuffix(v cc.Value) string {
	switch v.(type) {
	case cc.Int64Value, cc.UInt64Value:
		return " = " + vstr(v)
	}
	return ""
}

func vstr(v cc.Value) string {
	switch x := v.(type) {
	case cc.Int64Value:
		return fmt.Sprint(int64(x))
	case cc.UInt64Value:
		return fmt.Sprint(uint64(x))
	}
	return "?"
}

var kindNames = map[cc.Kind]string{
	cc.Bool: "_Bool", cc.Char: "char", cc.SChar: "signed char", cc.UChar: "unsigned char", cc.Short: "short",
	cc.UShort: "unsigned short", cc.Int: "int", cc.UInt: "unsigned", cc.Long: "long", cc.ULong: "unsigned long",
	cc.LongLong: "long long", cc.ULongLong: "unsigned long long", cc.Int128: "__int128", cc.UInt128: "unsigned __int128",
	cc.Float: "float", cc.Double: "double", cc.LongDouble: "long double", cc.Void: "void",
	cc.Float16: "_Float16", cc.Float32: "_Float32", cc.Float32x: "_Float32x", cc.Float64: "_Float64", cc.Float64x: "_Float64x",
	cc.Float128: "_Float128", cc.Float128x: "_Float128x", cc.BFloat16: "__bf16",
	cc.Decimal32: "_Decimal32", cc.Decimal64: "_Decimal64", cc.Decimal128: "_Decimal128",
	cc.ComplexChar: "_Complex char", cc.ComplexDouble: "_Complex double", cc.ComplexFloat: "_Complex float",
	cc.ComplexFloat16: "_Complex _Float16", cc.ComplexInt: "_Complex int", cc.ComplexLong: "_Complex long",
	cc.ComplexLongDouble: "_Complex long double", cc.ComplexLongLong: "_Complex long long",
	cc.ComplexShort: "_Complex short", cc.ComplexUInt: "_Complex unsigned", cc.ComplexUShort: "_Complex unsigned short",
}

func tstr(t cc.Type) string {
	var b strings.Builder
	tw(&b, t, 0)
	return b.String()
}

func tw(b *strings.Builder, t cc.Type, depth int) {
	if t == nil || depth > 32 {
		b.WriteString("?")
		return
	}
	switch x := t.(type) {
	case *cc.PointerType:
		b.WriteString("*")
		tw(b, x.Elem(), depth+1)
	case *cc.ArrayType:
		switch {
		case x.IsVLA():
			b.WriteString("[*]")
		case x.Len() < 0:
			b.WriteString("[]")
		default:
			fmt.Fprintf(b, "[%d]", x.Len())
		}
		tw(b, x.Elem(), depth+1)
	case *cc.FunctionType:
		b.WriteString("fn(")
		for i, p := range x.Parameters() {
			if i > 0 {
				b.WriteString(",")
			}
			tw(b, p.Type(), depth+1)
		}
		if x.IsVariadic() {
			if len(x.Parameters()) > 0 {
				b.WriteString(",")
			}
			b.WriteString("...")
		}
		b.WriteString(")")
		tw(b, x.Result(), depth+1)
	case *cc.StructType:
		b.WriteString("struct ")
		tag(b, x.Tag())
	case *cc.UnionType:
		b.WriteString("union ")
		tag(b, x.Tag())
	case *cc.EnumType:
		b.WriteString("enum ")
		tag(b, x.Tag())
	default:
		if n, ok := kindNames[t.Kind()]; ok {
			b.WriteString(n)
			return
		}
		b.WriteString("?")
	}
}

func tag(b *strings.Builder, t Token) {
	if s := t.SrcStr(); s != "" {
		b.WriteString(s)
		return
	}
	b.WriteString("#anon")
}

// firstLine is the line of the first token of the file `name` in a node (the least offset), 0 if none.
func firstLine(v reflect.Value, name string) int {
	best, line := -1, 0
	var walk func(v reflect.Value)
	walk = func(v reflect.Value) {
		for v.Kind() == reflect.Interface {
			if v.IsNil() {
				return
			}
			v = v.Elem()
		}
		if v.Kind() != reflect.Pointer || v.IsNil() || v.Elem().Kind() != reflect.Struct {
			return
		}
		s := v.Elem()
		t := s.Type()
		for i := 0; i < t.NumField(); i++ {
			f := t.Field(i)
			if !f.IsExported() {
				continue
			}
			fv := s.Field(i)
			switch {
			case f.Type == tokenType:
				tk := fv.Interface().(Token)
				if tk.Src() == nil {
					continue
				}
				if fn, ln := cc.ArbaceRawLine(tk); fn == name && ln > 0 {
					if p := tk.Position(); best < 0 || p.Offset < best {
						best, line = p.Offset, ln
					}
				}
			case fv.Kind() == reflect.Slice:
				for j := 0; j < fv.Len(); j++ {
					if x := fv.Index(j); x.Type() != tokenType {
						walk(x)
					}
				}
			default:
				walk(fv)
			}
		}
	}
	walk(v)
	return line
}
