package main

import (
	"go/types"
	"strings"
)

// The type table: every type a dump mentions is interned once, as structured data, and
// referred to by its index (an integer id) wherever an annotation carries a type. An entry is
// a map {:kind K ...} whose component types are ids of earlier entries, so the table is in
// dependency order (named types, aliases' names and type parameters are leaves: a named type's
// entry names it, it does not expand it). Named types and aliases are qualified by package
// path, and a local one also by its declaring position, so that two local types of one name
// are two entries. Each entry ends with :str, go/types' TypeString (for people; the package
// dumped unqualified), which is not part of its identity.
//
//	{:kind :basic :name "int"}                      ; also "byte", "rune", "untyped int", ...
//	{:kind :basic :name "Pointer" :pkg "unsafe"}    ; unsafe.Pointer
//	{:kind :pointer :elem E}  {:kind :slice :elem E}  {:kind :array :len N :elem E}
//	{:kind :map :key K :elem E}  {:kind :chan :dir :both|:send|:recv :elem E}
//	{:kind :func [:type-params [TP ...]] :params [V ...] :results [V ...] [:variadic true]}
//	{:kind :struct :fields [{:name F [:pkg P] :t T [:embedded true] [:tag "..."]} ...]}
//	{:kind :interface [:methods [{:name M [:pkg P] :t SIG} ...]] [:embeddeds [T ...]] [:implicit true]}
//	{:kind :union :terms [{:t T [:tilde true]} ...]}
//	{:kind :tuple :vars [V ...]}
//	{:kind :named :name N [:pkg P] [:decl POS] [:origin ID :args [T ...]]}
//	{:kind :alias :name N [:pkg P] [:decl POS] [:origin ID :args [T ...]] :actual T}
//	{:kind :type-param :name N :index I [:pkg P] [:decl POS]}
//
// with V = {[:name N] :t T} and TP = {:t TYPE-PARAM :constraint T}. :pkg is absent for the
// universe (error, comparable, any) and on fields and methods that are exported. :decl is
// written for local named types and aliases and for type parameters ("file.go:L:C"; for
// imported objects the file name is export data's). :origin and :args are an instance's
// generic type and type arguments. :actual is types.Unalias of an alias.
type typeTable struct {
	ids     map[string]int           // structural entries by their text (without :str)
	objs    map[types.Object]int     // generic or plain named types and aliases, by type name
	tparams map[*types.TypeParam]int // type parameters
	entries []string
}

func newTypeTable() *typeTable {
	return &typeTable{ids: map[string]int{}, objs: map[types.Object]int{}, tparams: map[*types.TypeParam]int{}}
}

func (tt *typeTable) add(key, str string) int {
	id := len(tt.entries)
	var b strings.Builder
	b.WriteString(key)
	b.WriteString(" :str ")
	out{&b}.str(str)
	b.WriteByte('}')
	tt.entries = append(tt.entries, b.String())
	return id
}

// typ returns the id of t in the dump's type table, interning it (and its components).
func (d *dumper) typ(t types.Type) int {
	tt := d.types
	var origin types.Object // set for a named type or alias that is not an instance
	switch x := t.(type) {
	case *types.TypeParam:
		if id, ok := tt.tparams[x]; ok {
			return id
		}
	case *types.Named:
		if x.Origin() == x {
			origin = x.Obj()
		}
	case *types.Alias:
		if x.Origin() == x {
			origin = x.Obj()
		}
	}
	if origin != nil {
		if id, ok := tt.objs[origin]; ok {
			return id
		}
	}
	var b strings.Builder
	w := out{&b}
	w.WriteString("{:kind ")
	switch x := t.(type) {
	case *types.Basic:
		w.WriteString(":basic :name ")
		if x.Kind() == types.UnsafePointer {
			w.WriteString(`"Pointer" :pkg "unsafe"`)
		} else {
			w.str(x.Name())
		}
	case *types.Pointer:
		w.WriteString(":pointer :elem ")
		w.int(int64(d.typ(x.Elem())))
	case *types.Slice:
		w.WriteString(":slice :elem ")
		w.int(int64(d.typ(x.Elem())))
	case *types.Array:
		w.WriteString(":array :len ")
		w.int(x.Len())
		w.WriteString(" :elem ")
		w.int(int64(d.typ(x.Elem())))
	case *types.Map:
		w.WriteString(":map :key ")
		w.int(int64(d.typ(x.Key())))
		w.WriteString(" :elem ")
		w.int(int64(d.typ(x.Elem())))
	case *types.Chan:
		w.WriteString(":chan :dir ")
		switch x.Dir() {
		case types.SendOnly:
			w.WriteString(":send")
		case types.RecvOnly:
			w.WriteString(":recv")
		default:
			w.WriteString(":both")
		}
		w.WriteString(" :elem ")
		w.int(int64(d.typ(x.Elem())))
	case *types.Signature:
		w.WriteString(":func")
		if tps := x.TypeParams(); tps.Len() > 0 {
			w.WriteString(" :type-params ")
			d.typeParams(w, tps)
		}
		w.WriteString(" :params ")
		d.vars(w, x.Params())
		if x.Results().Len() > 0 {
			w.WriteString(" :results ")
			d.vars(w, x.Results())
		}
		if x.Variadic() {
			w.WriteString(" :variadic true")
		}
	case *types.Struct:
		w.WriteString(":struct :fields [")
		for i := range x.NumFields() {
			f := x.Field(i)
			if i > 0 {
				w.WriteByte(' ')
			}
			w.WriteString("{:name ")
			w.str(f.Name())
			if !f.Exported() && f.Pkg() != nil {
				w.WriteString(" :pkg ")
				w.str(f.Pkg().Path())
			}
			w.WriteString(" :t ")
			w.int(int64(d.typ(f.Type())))
			if f.Embedded() {
				w.WriteString(" :embedded true")
			}
			if tag := x.Tag(i); tag != "" {
				w.WriteString(" :tag ")
				w.str(tag)
			}
			w.WriteByte('}')
		}
		w.WriteByte(']')
	case *types.Interface:
		w.WriteString(":interface")
		if x.NumExplicitMethods() > 0 {
			w.WriteString(" :methods [")
			for i := range x.NumExplicitMethods() {
				m := x.ExplicitMethod(i)
				if i > 0 {
					w.WriteByte(' ')
				}
				w.WriteString("{:name ")
				w.str(m.Name())
				if !m.Exported() && m.Pkg() != nil {
					w.WriteString(" :pkg ")
					w.str(m.Pkg().Path())
				}
				w.WriteString(" :t ")
				w.int(int64(d.typ(m.Type())))
				w.WriteByte('}')
			}
			w.WriteByte(']')
		}
		if x.NumEmbeddeds() > 0 {
			w.WriteString(" :embeddeds [")
			for i := range x.NumEmbeddeds() {
				if i > 0 {
					w.WriteByte(' ')
				}
				w.int(int64(d.typ(x.EmbeddedType(i))))
			}
			w.WriteByte(']')
		}
		if x.IsImplicit() {
			w.WriteString(" :implicit true")
		}
	case *types.Union:
		w.WriteString(":union :terms [")
		for i := range x.Len() {
			term := x.Term(i)
			if i > 0 {
				w.WriteByte(' ')
			}
			w.WriteString("{:t ")
			w.int(int64(d.typ(term.Type())))
			if term.Tilde() {
				w.WriteString(" :tilde true")
			}
			w.WriteByte('}')
		}
		w.WriteByte(']')
	case *types.Tuple:
		w.WriteString(":tuple :vars ")
		d.vars(w, x)
	case *types.Named:
		w.WriteString(":named")
		d.typeName(w, x.Obj())
		if x.Origin() != x {
			d.instance(w, x.Origin(), x.TypeArgs())
		}
	case *types.Alias:
		w.WriteString(":alias")
		d.typeName(w, x.Obj())
		if x.Origin() != x {
			d.instance(w, x.Origin(), x.TypeArgs())
		}
		w.WriteString(" :actual ")
		w.int(int64(d.typ(types.Unalias(x))))
	case *types.TypeParam:
		w.WriteString(":type-param :name ")
		w.str(x.Obj().Name())
		w.WriteString(" :index ")
		w.int(int64(x.Index()))
		if p := x.Obj().Pkg(); p != nil {
			w.WriteString(" :pkg ")
			w.str(p.Path())
		}
		if x.Obj().Pos().IsValid() {
			w.WriteString(" :decl ")
			w.str(d.posFile(x.Obj().Pos()))
		}
		id := tt.add(b.String(), d.tstr(t))
		tt.tparams[x] = id
		return id
	default:
		w.WriteString(":unknown")
	}
	key := b.String()
	if origin != nil {
		// a type name's entry is its own, even if another prints alike
		id := tt.add(key, d.tstr(t))
		tt.objs[origin] = id
		return id
	}
	if id, ok := tt.ids[key]; ok {
		return id
	}
	id := tt.add(key, d.tstr(t))
	tt.ids[key] = id
	return id
}

// typeName writes the name, package and (for local ones) declaring position of a named type
// or alias.
func (d *dumper) typeName(w out, tn *types.TypeName) {
	w.WriteString(" :name ")
	w.str(tn.Name())
	if p := tn.Pkg(); p != nil {
		w.WriteString(" :pkg ")
		w.str(p.Path())
		if tn.Parent() != p.Scope() && tn.Pos().IsValid() {
			w.WriteString(" :decl ")
			w.str(d.posFile(tn.Pos()))
		}
	}
}

func (d *dumper) instance(w out, origin types.Type, args *types.TypeList) {
	w.WriteString(" :origin ")
	w.int(int64(d.typ(origin)))
	w.WriteString(" :args [")
	for i := range args.Len() {
		if i > 0 {
			w.WriteByte(' ')
		}
		w.int(int64(d.typ(args.At(i))))
	}
	w.WriteByte(']')
}

// vars writes a tuple's variables: [{[:name N] :t T} ...].
func (d *dumper) vars(w out, t *types.Tuple) {
	w.WriteByte('[')
	for i := range t.Len() {
		v := t.At(i)
		if i > 0 {
			w.WriteByte(' ')
		}
		w.WriteByte('{')
		if v.Name() != "" {
			w.WriteString(":name ")
			w.str(v.Name())
			w.WriteByte(' ')
		}
		w.WriteString(":t ")
		w.int(int64(d.typ(v.Type())))
		w.WriteByte('}')
	}
	w.WriteByte(']')
}

// typeParams writes a type parameter list: [{:t TYPE-PARAM :constraint T} ...]. The type
// parameters are interned before their constraints, which may mention them.
func (d *dumper) typeParams(w out, tps *types.TypeParamList) {
	ids := make([]int, tps.Len())
	for i := range tps.Len() {
		ids[i] = d.typ(tps.At(i))
	}
	w.WriteByte('[')
	for i := range tps.Len() {
		if i > 0 {
			w.WriteByte(' ')
		}
		w.WriteString("{:t ")
		w.int(int64(ids[i]))
		w.WriteString(" :constraint ")
		w.int(int64(d.typ(tps.At(i).Constraint())))
		w.WriteByte('}')
	}
	w.WriteByte(']')
}

// writeTypeTable writes the table: one entry per line, the id being the index.
func (d *dumper) writeTypeTable() {
	w := d.w
	w.WriteString("\n :type-table [")
	for i, e := range d.types.entries {
		if i > 0 {
			w.WriteString("\n  ")
		}
		w.WriteString(e)
	}
	w.WriteString("]")
}
