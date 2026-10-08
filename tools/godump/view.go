package main

import (
	"go/ast"
	"go/types"
)

// annotate writes what go/types decided about node n as reader metadata ^{...} before its
// form, if anything:
//
//	:mode      :const :var (addressable) :mapindex :value :commaok :type :builtin :void :nil
//	:t         the type
//	:val       the constant value
//	:def :use  the object an identifier declares or denotes
//	:inst      the type arguments of a generic use, and the instantiated type
//	:sel       a selector's selection (field, method or method-expr; path; indirect; receiver)
//	:field     a struct literal key's field: its index path, and the embedded fields traversed
//	:implicit  the object a node declares implicitly (import specs, type switch clauses, ...)
func (d *dumper) annotate(n ast.Node) {
	info := d.info
	var tv types.TypeAndValue
	hasTV := false
	if e, ok := n.(ast.Expr); ok {
		tv, hasTV = info.Types[e]
	}
	id, _ := n.(*ast.Ident)
	var def, use types.Object
	var inst types.Instance
	hasInst := false
	if id != nil {
		def, use = info.Defs[id], info.Uses[id]
		inst, hasInst = info.Instances[id]
	}
	var sel *types.Selection
	if se, ok := n.(*ast.SelectorExpr); ok {
		sel = info.Selections[se]
	}
	if cl, ok := n.(*ast.CompositeLit); ok {
		d.litKeys(cl)
	}
	var kv *litKey
	if e, ok := n.(*ast.KeyValueExpr); ok {
		kv = d.keys[e]
	}
	impl := info.Implicits[n]
	if !hasTV && def == nil && use == nil && !hasInst && sel == nil && kv == nil && impl == nil {
		return
	}
	w := d.w
	w.WriteString("^{")
	sep := false
	key := func(k string) {
		if sep {
			w.WriteByte(' ')
		}
		sep = true
		w.WriteString(k)
	}
	if hasTV {
		key(":mode ")
		w.WriteString(mode(tv))
		if tv.Type != nil {
			key(":t ")
			d.tid(tv.Type)
		}
		if tv.Value != nil {
			key(":val ")
			w.val(tv.Value)
		}
	}
	if def != nil {
		key(":def ")
		d.obj(def, true)
	}
	if use != nil {
		key(":use ")
		d.obj(use, false)
	}
	if hasInst {
		key(":inst {:targs [")
		for i := range inst.TypeArgs.Len() {
			if i > 0 {
				w.WriteByte(' ')
			}
			d.tid(inst.TypeArgs.At(i))
		}
		w.WriteString("] :t ")
		d.tid(inst.Type)
		w.WriteString("}")
	}
	if sel != nil {
		key(":sel {:kind ")
		switch sel.Kind() {
		case types.FieldVal:
			w.WriteString(":field")
		case types.MethodVal:
			w.WriteString(":method")
		case types.MethodExpr:
			w.WriteString(":method-expr")
		}
		w.WriteString(" :path ")
		d.ints(sel.Index())
		w.WriteString(" :indirect ")
		w.bool(sel.Indirect())
		w.WriteString(" :recv ")
		d.tid(sel.Recv())
		if via := embeddedNames(sel.Recv(), sel.Index()); len(via) > 0 {
			w.WriteString(" :via ")
			d.strs(via)
		}
		w.WriteString("}")
	}
	if kv != nil {
		key(":field {:path ")
		d.ints(kv.path)
		if len(kv.via) > 0 {
			w.WriteString(" :via ")
			d.strs(kv.via)
		}
		w.WriteString("}")
	}
	if impl != nil {
		key(":implicit ")
		d.obj(impl, true)
	}
	w.WriteString("} ")
}

func mode(tv types.TypeAndValue) string {
	switch {
	case tv.IsVoid():
		return ":void"
	case tv.IsType():
		return ":type"
	case tv.IsBuiltin():
		return ":builtin"
	case tv.IsNil():
		return ":nil"
	case tv.Value != nil:
		return ":const"
	case tv.Addressable():
		return ":var"
	case tv.Assignable():
		return ":mapindex"
	case tv.HasOk():
		return ":commaok"
	}
	return ":value"
}

func (d *dumper) ints(is []int) {
	w := d.w
	w.WriteByte('[')
	for i, x := range is {
		if i > 0 {
			w.WriteByte(' ')
		}
		w.int(int64(x))
	}
	w.WriteByte(']')
}

// obj writes an object: {:kind K :name N ...}
//
//	:pkg        its package's path, when not the package dumped
//	:decl       its position, for objects of the package dumped
//	:t          its type (not for package names and labels)
//	:pkg-level  declared in its package's scope
//	:universe   declared in the universe (int, len, nil, true, ...)
//	:recv       a method's receiver type
//	:embedded   an embedded field
//	:alias :type-param  type names that are aliases or type parameters
//	:path       a package name's import path
//	:val        a constant's value, on definitions
func (d *dumper) obj(o types.Object, isDef bool) {
	w := d.w
	w.WriteString("{:kind ")
	switch x := o.(type) {
	case *types.Var:
		if x.IsField() {
			w.WriteString(":field")
		} else {
			w.WriteString(":var")
		}
	case *types.Func:
		w.WriteString(":func")
	case *types.Const:
		w.WriteString(":const")
	case *types.TypeName:
		w.WriteString(":type")
	case *types.PkgName:
		w.WriteString(":pkg")
	case *types.Builtin:
		w.WriteString(":builtin")
	case *types.Label:
		w.WriteString(":label")
	case *types.Nil:
		w.WriteString(":nil")
	default:
		w.WriteString(":unknown")
	}
	w.WriteString(" :name ")
	w.str(o.Name())
	if o.Pkg() != nil && o.Pkg() != d.pkg {
		w.WriteString(" :pkg ")
		w.str(o.Pkg().Path())
	}
	if o.Pkg() == d.pkg && o.Pos().IsValid() {
		w.WriteString(" :decl ")
		d.pos(o.Pos())
	}
	switch x := o.(type) {
	case *types.PkgName:
		w.WriteString(" :path ")
		w.str(x.Imported().Path())
	case *types.Label:
	default:
		if t := o.Type(); t != nil {
			w.WriteString(" :t ")
			d.tid(t)
		}
	}
	if p := o.Parent(); p != nil {
		if p == types.Universe {
			w.WriteString(" :universe true")
		} else if o.Pkg() != nil && p == o.Pkg().Scope() {
			w.WriteString(" :pkg-level true")
		}
	}
	switch x := o.(type) {
	case *types.Func:
		if sig, ok := x.Type().(*types.Signature); ok && sig.Recv() != nil {
			w.WriteString(" :recv ")
			d.tid(sig.Recv().Type())
		}
	case *types.Var:
		if x.Embedded() {
			w.WriteString(" :embedded true")
		}
	case *types.TypeName:
		if x.IsAlias() {
			w.WriteString(" :alias true")
		}
		if _, ok := x.Type().(*types.TypeParam); ok {
			w.WriteString(" :type-param true")
		}
		if _, ok := x.Type().(*types.TypeParam); isDef && x.Pkg() == d.pkg && !ok {
			d.names = append(d.names, x)
		}
	case *types.Const:
		if isDef {
			w.WriteString(" :val ")
			w.val(x.Val())
		}
	}
	w.WriteString("}")
}

// typeView writes a named type (or alias) declared in the package, at package level or local:
//
//	{:name N :decl POS [:local true] :t T :underlying U
//	 [:alias true :rhs R] [:type-params [{:t TP :constraint C} ...]]
//	 :method-set [M ...] [:ptr-method-set [M ...]]
//	 [:size S :align A [:fields [{:name F :t T :offset O :size S :align A [:embedded true]} ...]]]}
//
// with M = {:name N [:pkg P] :t SIG :recv R :path [I ...] :indirect B}, every type an id in the
// type table (types.go). Method sets are go/types'
// (sorted by name and package). Layouts are types.SizesFor("gc", GOARCH)'s, for types without
// type parameters.
func (d *dumper) typeView(tn *types.TypeName) {
	w := d.w
	w.WriteString("{:name ")
	w.str(tn.Name())
	w.WriteString(" :decl ")
	w.str(d.posFile(tn.Pos()))
	if tn.Parent() != d.pkg.Scope() {
		w.WriteString(" :local true")
	}
	t := tn.Type()
	w.WriteString(" :t ")
	d.tid(t)
	if tn.IsAlias() {
		w.WriteString(" :alias true :rhs ")
		if a, ok := t.(*types.Alias); ok {
			d.tid(a.Rhs())
		} else {
			d.tid(t)
		}
		if a, ok := t.(*types.Alias); ok && a.TypeParams().Len() > 0 {
			d.tparams(a.TypeParams())
		}
		w.WriteString("}")
		return
	}
	w.WriteString(" :underlying ")
	d.tid(t.Underlying())
	named, _ := t.(*types.Named)
	if named != nil && named.TypeParams().Len() > 0 {
		d.tparams(named.TypeParams())
	}
	w.WriteString("\n   :method-set ")
	d.methodSet(types.NewMethodSet(t))
	if !types.IsInterface(t) {
		w.WriteString("\n   :ptr-method-set ")
		d.methodSet(types.NewMethodSet(types.NewPointer(t)))
	}
	d.layout(t)
	w.WriteString("}")
}

func (d *dumper) tparams(tps *types.TypeParamList) {
	d.w.WriteString(" :type-params ")
	d.typeParams(d.w, tps)
}

func (d *dumper) methodSet(ms *types.MethodSet) {
	w := d.w
	w.WriteByte('[')
	for i := range ms.Len() {
		if i > 0 {
			w.WriteByte(' ')
		}
		s := ms.At(i)
		f := s.Obj()
		w.WriteString("{:name ")
		w.str(f.Name())
		if f.Pkg() != nil && f.Pkg() != d.pkg {
			w.WriteString(" :pkg ")
			w.str(f.Pkg().Path())
		}
		w.WriteString(" :t ")
		d.tid(f.Type())
		if sig, ok := f.Type().(*types.Signature); ok && sig.Recv() != nil {
			w.WriteString(" :recv ")
			d.tid(sig.Recv().Type())
		}
		w.WriteString(" :path ")
		d.ints(s.Index())
		w.WriteString(" :indirect ")
		w.bool(s.Indirect())
		w.WriteString("}")
	}
	w.WriteByte(']')
}

// layout writes size, alignment and (for structs) field offsets, unless the type depends on
// type parameters (or is invalid), where gc's sizes are undefined.
func (d *dumper) layout(t types.Type) {
	if dependsOnTypeParams(t, nil) {
		return
	}
	var size, align int64
	var offsets []int64
	ok := func() (ok bool) {
		defer func() {
			if recover() != nil {
				ok = false
			}
		}()
		size, align = d.sizes.Sizeof(t), d.sizes.Alignof(t)
		if st, isStruct := t.Underlying().(*types.Struct); isStruct {
			var fs []*types.Var
			for i := range st.NumFields() {
				fs = append(fs, st.Field(i))
			}
			offsets = d.sizes.Offsetsof(fs)
		}
		return true
	}()
	if !ok {
		return
	}
	w := d.w
	w.WriteString("\n   :size ")
	w.int(size)
	w.WriteString(" :align ")
	w.int(align)
	st, isStruct := t.Underlying().(*types.Struct)
	if !isStruct {
		return
	}
	w.WriteString(" :fields [")
	for i := range st.NumFields() {
		f := st.Field(i)
		if i > 0 {
			w.WriteByte(' ')
		}
		w.WriteString("{:name ")
		w.str(f.Name())
		w.WriteString(" :t ")
		d.tid(f.Type())
		w.WriteString(" :offset ")
		w.int(offsets[i])
		w.WriteString(" :size ")
		w.int(d.sizes.Sizeof(f.Type()))
		w.WriteString(" :align ")
		w.int(d.sizes.Alignof(f.Type()))
		if f.Embedded() {
			w.WriteString(" :embedded true")
		}
		w.WriteString("}")
	}
	w.WriteString("]")
}

// dependsOnTypeParams reports whether t mentions a type parameter (or is invalid).
func dependsOnTypeParams(t types.Type, seen map[types.Type]bool) bool {
	if seen[t] {
		return false
	}
	if seen == nil {
		seen = map[types.Type]bool{}
	}
	seen[t] = true
	switch x := t.(type) {
	case *types.TypeParam:
		return true
	case *types.Basic:
		return x.Kind() == types.Invalid
	case *types.Named:
		if x.TypeParams().Len() > 0 && x.TypeArgs().Len() == 0 {
			return true
		}
		for i := range x.TypeArgs().Len() {
			if dependsOnTypeParams(x.TypeArgs().At(i), seen) {
				return true
			}
		}
		return dependsOnTypeParams(x.Underlying(), seen)
	case *types.Alias:
		return dependsOnTypeParams(types.Unalias(x), seen)
	case *types.Pointer, *types.Slice, *types.Map, *types.Chan, *types.Signature, *types.Interface:
		return false // fixed size whatever they point to
	case *types.Array:
		return dependsOnTypeParams(x.Elem(), seen)
	case *types.Struct:
		for i := range x.NumFields() {
			if dependsOnTypeParams(x.Field(i).Type(), seen) {
				return true
			}
		}
	}
	return false
}

// tid writes the type table id of t.
func (d *dumper) tid(t types.Type) {
	d.w.int(int64(d.typ(t)))
}

// A litKey is the field a key of a struct literal selects (go/types records only the field
// object): its index path through embedded fields and the names of the embedded fields.
type litKey struct {
	path []int
	via  []string
}

// litKeys finds the fields of the keys of a struct literal, before its elements are written.
func (d *dumper) litKeys(cl *ast.CompositeLit) {
	tv, ok := d.info.Types[cl]
	if !ok || tv.Type == nil || len(cl.Elts) == 0 {
		return
	}
	st := structOf(tv.Type)
	if st == nil {
		return
	}
	for _, e := range cl.Elts {
		kv, ok := e.(*ast.KeyValueExpr)
		if !ok {
			continue
		}
		id, ok := kv.Key.(*ast.Ident)
		if !ok {
			continue
		}
		f, ok := d.info.Uses[id].(*types.Var)
		if !ok || !f.IsField() {
			continue
		}
		obj, index, _ := types.LookupFieldOrMethod(st, false, f.Pkg(), f.Name())
		if obj != f {
			continue
		}
		if d.keys == nil {
			d.keys = map[*ast.KeyValueExpr]*litKey{}
		}
		d.keys[kv] = &litKey{path: index, via: embeddedNames(st, index)}
	}
}

// structOf is the struct type behind t: through aliases, names, one pointer (an elided &T in
// a literal, a selection's receiver) and a type parameter's common underlying type.
func structOf(t types.Type) *types.Struct {
	t = types.Unalias(t)
	if p, ok := t.Underlying().(*types.Pointer); ok {
		t = types.Unalias(p.Elem())
	}
	if tp, ok := t.(*types.TypeParam); ok {
		return commonStruct(tp.Constraint(), nil)
	}
	st, _ := t.Underlying().(*types.Struct)
	return st
}

// commonStruct is the struct type of a constraint's type set, when its terms have one.
func commonStruct(t types.Type, seen map[types.Type]bool) *types.Struct {
	if seen[t] {
		return nil
	}
	if seen == nil {
		seen = map[types.Type]bool{}
	}
	seen[t] = true
	switch u := t.Underlying().(type) {
	case *types.Struct:
		return u
	case *types.Interface:
		for i := range u.NumEmbeddeds() {
			if st := commonStruct(u.EmbeddedType(i), seen); st != nil {
				return st
			}
		}
	case *types.Union:
		for i := range u.Len() {
			if st := commonStruct(u.Term(i).Type(), seen); st != nil {
				return st
			}
		}
	}
	return nil
}

// embeddedNames are the names of the embedded fields an index path (of a selection or a
// literal key) traverses from t: all its indexes but the last.
func embeddedNames(t types.Type, index []int) []string {
	var names []string
	for _, i := range index[:max(0, len(index)-1)] {
		st := structOf(t)
		if st == nil || i >= st.NumFields() {
			return nil
		}
		f := st.Field(i)
		names = append(names, f.Name())
		t = f.Type()
	}
	return names
}
