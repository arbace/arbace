package main

// Level 2: the export data.
//
// gc writes a package's export data (types, method sets, constants, the
// bodies of inlinable and generic functions) in the Unified IR format of
// internal/pkgbits into the __.PKGDEF member of the package archive. Both
// sides are compiled with -d=syncframes=0, which makes the format
// self-describing: every primitive value is preceded by a sync marker giving
// its kind, and every structural step by a marker naming it. This file decodes
// that generic structure (it does not need the compiler's grammar) and compares
// the two element graphs from their roots, following references, with
// positions (marker Pos and what follows it) left out.

import (
	"bytes"
	"encoding/binary"
	"errors"
	"fmt"
	"math/big"
	"os"
	"sort"
	"strconv"
	"strings"
)

// The sync markers of internal/pkgbits (sync.go, go1.27). Only the first ones
// are interpreted; the rest are compared as opaque markers and named in
// messages.
const (
	syncEOF      = 1
	syncBool     = 2
	syncInt64    = 3
	syncUint64   = 4
	syncString   = 5
	syncValue    = 6
	syncVal      = 7
	syncRelocs   = 8
	syncReloc    = 9
	syncUseReloc = 10
	syncPos      = 12
	syncPkgDef   = 17
	syncType     = 19
)

var syncNames = strings.Fields(`? EOF Bool Int64 Uint64 String Value Val Relocs Reloc UseReloc
Public Pos PosBase Object Object1 Pkg PkgDef Method Type TypeIdx TypeParamNames Signature Params
Param CodeObj Sym LocalIdent Selector Private FuncExt VarExt TypeExt Pragma ExprList Exprs Expr
ExprType Assign Op FuncLit CompLit Decl FuncBody OpenScope CloseScope CloseAnotherScope DeclNames
DeclName Stmts Block IfStmt ForStmt SwitchStmt RangeStmt CaseClause CommClause SelectStmt Decls
LabeledStmt UseObjLocal AddLocal Linkname Stmt1 StmtsEnd Label OptLabel MultiExpr RType Convert
RTTI`)

func syncName(m uint64) string {
	if m < uint64(len(syncNames)) {
		return syncNames[m]
	}
	return "Sync" + strconv.FormatUint(m, 10)
}

// Sections of internal/pkgbits (reloc.go).
const (
	secString  = 0
	secMeta    = 1
	secPosBase = 2
	secPkg     = 3
	secName    = 4
	secType    = 5
	secObj     = 6
	secObjExt  = 7
	secObjDict = 8
	numSecs    = 10
)

var secNames = []string{"String", "Meta", "PosBase", "Pkg", "Name", "Type", "Obj", "ObjExt", "ObjDict", "Body"}

// unified is one package's decoded export data.
type unified struct {
	header       string // the first line of __.PKGDEF ("go object GOOS GOARCH VERSION ...")
	version      uint32
	secEnds      [numSecs]uint32
	elemEnds     []uint32
	data         string
	elems        map[int][]tok // decoded elements, by absolute index
	derivedCache map[int][]int // derived types of dictionaries
}

type tokKind uint8

const (
	tMarker tokKind = iota
	tBool
	tInt
	tUint
	tRef    // a reference to another element (not a string)
	tString // a string, resolved
	tPos    // a position, whatever its value
)

type tok struct {
	kind tokKind
	m    uint64 // marker, for tMarker
	v    uint64 // value; for tRef the absolute element index
	s    string
	sec  int // section, for tRef
}

func (t tok) String() string {
	switch t.kind {
	case tMarker:
		return syncName(t.m)
	case tBool:
		return fmt.Sprintf("bool %d", t.v)
	case tInt:
		return fmt.Sprintf("int %d", int64(t.v))
	case tUint:
		return fmt.Sprintf("uint %d", t.v)
	case tRef:
		return fmt.Sprintf("-> %s", secNames[t.sec])
	case tString:
		return strconv.Quote(t.s)
	case tPos:
		return "pos"
	}
	return "?"
}

// readArchiveMember returns the named member of a Go archive (!<arch>).
func readArchiveMember(path, name string) ([]byte, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	const magic = "!<arch>\n"
	if !bytes.HasPrefix(b, []byte(magic)) {
		return nil, fmt.Errorf("%s: not an archive", path)
	}
	off := len(magic)
	for off+60 <= len(b) {
		hdr := b[off : off+60]
		mname := strings.TrimSpace(string(hdr[0:16]))
		size, err := strconv.Atoi(strings.TrimSpace(string(hdr[48:58])))
		if err != nil {
			return nil, fmt.Errorf("%s: bad archive header", path)
		}
		off += 60
		if off+size > len(b) {
			return nil, fmt.Errorf("%s: truncated archive", path)
		}
		if mname == name {
			return b[off : off+size], nil
		}
		off += size + size%2
	}
	return nil, fmt.Errorf("%s: no member %s", path, name)
}

func loadUnified(archive string) (*unified, error) {
	def, err := readArchiveMember(archive, "__.PKGDEF")
	if err != nil {
		return nil, err
	}
	i := bytes.Index(def, []byte("\n$$B\n"))
	if i < 0 {
		return nil, fmt.Errorf("%s: no export data", archive)
	}
	u := &unified{elems: map[int][]tok{}, derivedCache: map[int][]int{}}
	u.header, _, _ = strings.Cut(string(def[:i]), "\n")
	rest := def[i+5:]
	if len(rest) == 0 || rest[0] != 'u' {
		return nil, fmt.Errorf("%s: export data is not Unified IR", archive)
	}
	r := bytes.NewReader(rest[1:])
	if err := binary.Read(r, binary.LittleEndian, &u.version); err != nil {
		return nil, err
	}
	var flags uint32
	if u.version >= 1 {
		if err := binary.Read(r, binary.LittleEndian, &flags); err != nil {
			return nil, err
		}
	}
	if flags&1 == 0 {
		return nil, fmt.Errorf("%s: export data without sync markers (compile with -d=syncframes=0)", archive)
	}
	if err := binary.Read(r, binary.LittleEndian, u.secEnds[:]); err != nil {
		return nil, err
	}
	u.elemEnds = make([]uint32, u.secEnds[numSecs-1])
	if err := binary.Read(r, binary.LittleEndian, u.elemEnds); err != nil {
		return nil, err
	}
	start := len(rest) - 1 - r.Len() + 1
	end := start
	if len(u.elemEnds) > 0 {
		end += int(u.elemEnds[len(u.elemEnds)-1])
	}
	if end > len(rest) {
		return nil, fmt.Errorf("%s: truncated export data", archive)
	}
	u.data = string(rest[start:end])
	return u, nil
}

func (u *unified) absIdx(sec int, idx uint64) (int, error) {
	abs := int(idx)
	if sec > 0 {
		abs += int(u.secEnds[sec-1])
	}
	if abs >= int(u.secEnds[sec]) {
		return 0, fmt.Errorf("element %s[%d] out of range", secNames[sec], idx)
	}
	return abs, nil
}

func (u *unified) raw(abs int) string {
	var s uint32
	if abs > 0 {
		s = u.elemEnds[abs-1]
	}
	return u.data[s:u.elemEnds[abs]]
}

func (u *unified) section(abs int) int {
	for s := range numSecs {
		if abs < int(u.secEnds[s]) {
			return s
		}
	}
	return -1
}

// elem decodes the element with the given absolute index into tokens.
func (u *unified) elem(abs int) ([]tok, error) {
	if t, ok := u.elems[abs]; ok {
		return t, nil
	}
	data := u.raw(abs)
	p := 0
	uv := func() (uint64, error) {
		x, n := binary.Uvarint([]byte(data[p:min(len(data), p+binary.MaxVarintLen64)]))
		if n <= 0 {
			return 0, fmt.Errorf("element %d (%s) at byte %d of %d: bad varint", abs, secNames[u.section(abs)], p, len(data))
		}
		p += n
		return x, nil
	}
	var raw []tok
	for p < len(data) {
		m, err := uv()
		if err != nil {
			return nil, err
		}
		nf, err := uv()
		if err != nil {
			return nil, err
		}
		for range nf {
			if _, err := uv(); err != nil {
				return nil, err
			}
		}
		switch m {
		case syncBool:
			if p >= len(data) {
				return nil, errors.New("truncated bool")
			}
			raw = append(raw, tok{kind: tBool, v: uint64(data[p])})
			p++
		case syncInt64:
			x, err := uv()
			if err != nil {
				return nil, err
			}
			v := int64(x >> 1)
			if x&1 != 0 {
				v = ^v
			}
			raw = append(raw, tok{kind: tInt, v: uint64(v)})
		case syncUint64:
			x, err := uv()
			if err != nil {
				return nil, err
			}
			raw = append(raw, tok{kind: tUint, v: x})
		default:
			raw = append(raw, tok{kind: tMarker, m: m})
		}
	}
	// The relocation header: Relocs, Len n, n times (Reloc, Len kind, Len idx).
	bad := fmt.Errorf("element %d: bad relocation header", abs)
	if len(raw) < 2 || raw[0].kind != tMarker || raw[0].m != syncRelocs || raw[1].kind != tUint {
		return nil, bad
	}
	n := int(raw[1].v)
	if len(raw) < 2+3*n {
		return nil, bad
	}
	type reloc struct {
		sec int
		idx uint64
	}
	relocs := make([]reloc, n)
	for i := range n {
		r := raw[2+3*i : 5+3*i]
		if r[0].kind != tMarker || r[0].m != syncReloc || r[1].kind != tUint || r[2].kind != tUint || r[1].v >= numSecs {
			return nil, bad
		}
		relocs[i] = reloc{int(r[1].v), r[2].v}
	}
	raw = raw[2+3*n:]
	// Resolve references and strings, and fold positions.
	var out []tok
	for i := 0; i < len(raw); i++ {
		t := raw[i]
		switch {
		case t.kind == tMarker && t.m == syncUseReloc:
			if i+1 >= len(raw) || raw[i+1].kind != tUint || raw[i+1].v >= uint64(n) {
				return nil, fmt.Errorf("element %d: bad reference", abs)
			}
			r := relocs[raw[i+1].v]
			i++
			ra, err := u.absIdx(r.sec, r.idx)
			if err != nil {
				return nil, err
			}
			if r.sec == secString {
				out = append(out, tok{kind: tString, s: u.raw(ra)})
			} else {
				out = append(out, tok{kind: tRef, sec: r.sec, v: uint64(ra)})
			}
		case t.kind == tMarker && t.m == syncString:
			// the next token is the string's reference; the marker adds nothing
		case t.kind == tMarker && t.m == syncPos:
			// Pos, Bool known, [UseReloc PosBase, Uint line, Uint col]
			if i+1 < len(raw) && raw[i+1].kind == tBool {
				i++
				if raw[i].v != 0 {
					i += 4 // UseReloc, Uint, Uint, Uint
				}
			}
			out = append(out, tok{kind: tPos})
		default:
			out = append(out, t)
		}
	}
	if u.section(abs) == secPkg {
		if err := u.sortImports(out); err != nil {
			return nil, err
		}
	}
	out = foldValues(out)
	u.elems[abs] = out
	return out, nil
}

// foldValues replaces each constant (Value, Bool complex, then one or two
// scalars Val, Uint kind, ...) by a string token holding its exact value. gc
// writes a constant as int64, big integer, rational or float depending on how
// go/constant happened to represent it, which depends on how the value was
// spelled and computed (1000 and 1e3, an int64 that went through a big
// computation), not on the value.
func foldValues(ts []tok) []tok {
	var out []tok
	for i := 0; i < len(ts); i++ {
		if ts[i].kind != tMarker || ts[i].m != syncValue || i+1 >= len(ts) || ts[i+1].kind != tBool {
			out = append(out, ts[i])
			continue
		}
		n := 1 + int(ts[i+1].v) // scalars
		j := i + 2
		var parts []string
		ok := true
		for range n {
			v, next, good := scalarValue(ts, j)
			if !good {
				ok = false
				break
			}
			parts = append(parts, v)
			j = next
		}
		if !ok {
			out = append(out, ts[i])
			continue
		}
		out = append(out, tok{kind: tString, s: "const " + strings.Join(parts, " ")})
		i = j - 1
	}
	return out
}

// scalarValue reads one scalar at ts[j] (Val, Uint kind, data) and returns
// its exact value as text and the index after it.
func scalarValue(ts []tok, j int) (string, int, bool) {
	if j+1 >= len(ts) || ts[j].kind != tMarker || ts[j].m != syncVal || ts[j+1].kind != tUint {
		return "", 0, false
	}
	j += 2
	at := func(k tokKind) bool { return j < len(ts) && ts[j].kind == k }
	bigInt := func() (*big.Int, bool) {
		if !at(tString) || j+1 >= len(ts) || ts[j+1].kind != tBool {
			return nil, false
		}
		x := new(big.Int).SetBytes([]byte(ts[j].s))
		if ts[j+1].v != 0 {
			x.Neg(x)
		}
		j += 2
		return x, true
	}
	switch ts[j-1].v {
	case 0: // bool
		if !at(tBool) {
			return "", 0, false
		}
		return fmt.Sprintf("bool %d", ts[j].v), j + 1, true
	case 1: // string
		if !at(tString) {
			return "", 0, false
		}
		return "string " + strconv.Quote(ts[j].s), j + 1, true
	case 2: // int64
		if !at(tInt) {
			return "", 0, false
		}
		return "num " + strconv.FormatInt(int64(ts[j].v), 10), j + 1, true
	case 3: // big.Int
		x, ok := bigInt()
		if !ok {
			return "", 0, false
		}
		return "num " + x.String(), j, true
	case 4: // big.Rat
		num, ok1 := bigInt()
		den, ok2 := bigInt()
		if !ok1 || !ok2 || den.Sign() == 0 {
			return "", 0, false
		}
		return "num " + ratString(new(big.Rat).SetFrac(num, den)), j, true
	case 5: // big.Float
		if !at(tString) {
			return "", 0, false
		}
		f, _, err := big.ParseFloat(ts[j].s, 0, 4096, big.ToNearestEven)
		if err != nil || f.IsInf() {
			return "", 0, false
		}
		r, _ := f.Rat(nil)
		return "num " + ratString(r), j + 1, true
	}
	return "", 0, false
}

func ratString(r *big.Rat) string {
	if r.IsInt() {
		return r.Num().String()
	}
	return r.String()
}

// sortImports puts the imports of a package element in the order of their
// paths. An element is PkgDef, path, name, Len n, then n times (Pkg, ref);
// the compiler writes the imports in the order the source first mentions
// them, which carries no meaning: the linker orders initialization by the
// dependency graph and then by path (ROUNDTRIP.md, "Level 2").
func (u *unified) sortImports(ts []tok) error {
	if len(ts) < 4 || ts[0].kind != tMarker || ts[0].m != syncPkgDef || ts[3].kind != tUint {
		return nil // builtin, unsafe
	}
	n := int(ts[3].v)
	if len(ts) < 4+2*n {
		return fmt.Errorf("bad package element")
	}
	type imp struct {
		path string
		pair [2]tok
	}
	imps := make([]imp, n)
	for i := range n {
		pair := [2]tok{ts[4+2*i], ts[5+2*i]}
		if pair[1].kind != tRef {
			return fmt.Errorf("bad package element")
		}
		sub, err := u.elem(int(pair[1].v))
		if err != nil {
			return err
		}
		path := ""
		if len(sub) > 1 && sub[1].kind == tString {
			path = sub[1].s
		}
		imps[i] = imp{path, pair}
	}
	sort.SliceStable(imps, func(i, j int) bool { return imps[i].path < imps[j].path })
	for i, im := range imps {
		ts[4+2*i], ts[5+2*i] = im.pair[0], im.pair[1]
	}
	return nil
}

// describe names an element for messages: its section, index and the first
// strings in it or in the elements it refers to directly.
func (u *unified) describe(abs int) string {
	sec := u.section(abs)
	s := fmt.Sprintf("%s[%d]", secNames[sec], abs-int(u.sectionStart(sec)))
	toks, err := u.elem(abs)
	if err != nil {
		return s
	}
	var names []string
	for _, t := range toks {
		if len(names) >= 3 {
			break
		}
		switch t.kind {
		case tString:
			if t.s != "" {
				names = append(names, strconv.Quote(t.s))
			}
		case tRef:
			if t.sec == 3 || t.sec == 4 { // Pkg, Name
				if sub, err := u.elem(int(t.v)); err == nil {
					for _, st := range sub {
						if st.kind == tString && st.s != "" {
							names = append(names, strconv.Quote(st.s))
							break
						}
					}
				}
			}
		}
	}
	if len(names) > 0 {
		s += " " + strings.Join(names, " ")
	}
	return s
}

func (u *unified) sectionStart(sec int) uint32 {
	if sec == 0 {
		return 0
	}
	return u.secEnds[sec-1]
}

type exportCmp struct {
	a, b *unified
	memo map[[4]int]bool
	n    int // elements compared
}

type exportDiff struct {
	path []string // innermost first
	msg  string
}

func compareExport(archA, archB string) (string, error) {
	a, err := loadUnified(archA)
	if err != nil {
		return "", err
	}
	b, err := loadUnified(archB)
	if err != nil {
		return "", err
	}
	if a.header != b.header {
		return fmt.Sprintf("object headers differ:\n  A %s\n  B %s", a.header, b.header), nil
	}
	if a.version != b.version {
		return fmt.Sprintf("export data versions differ: %d vs %d", a.version, b.version), nil
	}
	c := &exportCmp{a: a, b: b, memo: map[[4]int]bool{}}
	na := int(a.secEnds[secMeta] - a.secEnds[secString])
	nb := int(b.secEnds[secMeta] - b.secEnds[secString])
	if na != nb {
		return fmt.Sprintf("number of roots: %d vs %d", na, nb), nil
	}
	for i := range na {
		ia, _ := a.absIdx(secMeta, uint64(i))
		ib, _ := b.absIdx(secMeta, uint64(i))
		d, err := c.cmp(ia, ib, noDict)
		if err != nil {
			return "", err
		}
		if d != nil {
			var sb strings.Builder
			fmt.Fprintf(&sb, "%s\n  path (root first):", d.msg)
			for j := len(d.path) - 1; j >= 0; j-- {
				sb.WriteString("\n    " + d.path[j])
			}
			return sb.String(), nil
		}
	}
	return "", nil
}

// noDict is the context of elements outside any generic object.
var noDict = [2]int{-1, -1}

// cmp compares two elements and, through references, everything they reach.
// A pair under comparison counts as equal when met again (the graphs have
// cycles), which makes the result the largest bisimulation.
//
// An object (section Obj) has companions at the same index in the sections
// Name, ObjExt (pragmas, linkname, inlining cost and body) and ObjDict (its
// dictionary), which no reference names: they are compared with it. Inside
// a generic object, a derived type (Type, Bool true, index) is an index into
// the object's dictionary, numbered in the order the compiler met the types;
// it is compared by the type it stands for. dict is the pair of dictionaries
// in effect.
func (c *exportCmp) cmp(ia, ib int, dict [2]int) (*exportDiff, error) {
	key := [4]int{ia, ib, dict[0], dict[1]}
	if _, ok := c.memo[key]; ok {
		return nil, nil
	}
	c.memo[key] = true
	c.n++
	if c.a.section(ia) == secObj && c.b.section(ib) == secObj {
		ra, rb := ia-int(c.a.sectionStart(secObj)), ib-int(c.b.sectionStart(secObj))
		abs := func(u *unified, sec, rel int) int { return int(u.sectionStart(sec)) + rel }
		dict = [2]int{abs(c.a, secObjDict, ra), abs(c.b, secObjDict, rb)}
		for _, sec := range []int{secName, secObjDict, secObjExt} {
			d, err := c.cmp(abs(c.a, sec, ra), abs(c.b, sec, rb), dict)
			if err != nil || d != nil {
				if d != nil {
					d.path = append(d.path, fmt.Sprintf("%s / %s, its %s", c.a.describe(ia), c.b.describe(ib), secNames[sec]))
				}
				return d, err
			}
		}
	}
	ta, err := c.a.elem(ia)
	if err != nil {
		return nil, err
	}
	tb, err := c.b.elem(ib)
	if err != nil {
		return nil, err
	}
	here := func(i int) string {
		return fmt.Sprintf("%s / %s, token %d", c.a.describe(ia), c.b.describe(ib), i)
	}
	mismatch := func(i int, what string) *exportDiff {
		return &exportDiff{
			msg:  fmt.Sprintf("%s\n  A ... %s\n  B ... %s", what, context(ta, i), context(tb, i)),
			path: []string{here(i)},
		}
	}
	for i := 0; i < min(len(ta), len(tb)); i++ {
		x, y := ta[i], tb[i]
		same := x.kind == y.kind
		if same {
			switch x.kind {
			case tMarker:
				same = x.m == y.m
			case tBool, tInt, tUint:
				same = x.v == y.v
			case tString:
				same = x.s == y.s
			case tRef:
				same = x.sec == y.sec
			}
		}
		if !same && x.kind == tString && y.kind == tString && c.sameRounded(ta, tb, i) {
			same = true
		}
		if !same {
			return mismatch(i, fmt.Sprintf("%s vs %s", x, y)), nil
		}
		next := func(j int, k tokKind, v uint64) bool {
			return j < len(ta) && j < len(tb) && ta[j].kind == k && tb[j].kind == k && (k == tUint || ta[j].v == v && tb[j].v == v)
		}
		if x.kind == tMarker && x.m == syncType && dict != noDict && next(i+1, tBool, 1) && next(i+2, tUint, 0) {
			da, err1 := c.a.derived(dict[0])
			db, err2 := c.b.derived(dict[1])
			if err := errors.Join(err1, err2); err != nil {
				return nil, err
			}
			ka, kb := int(ta[i+2].v), int(tb[i+2].v)
			if ka >= len(da) || kb >= len(db) {
				return mismatch(i+2, "derived type index out of range"), nil
			}
			d, err := c.cmp(da[ka], db[kb], dict)
			if err != nil || d != nil {
				if d != nil {
					d.path = append(d.path, here(i)+fmt.Sprintf(" (derived types %d / %d)", ka, kb))
				}
				return d, err
			}
			i += 2
			continue
		}
		if x.kind == tRef {
			sub := dict
			if x.sec == secObj {
				sub = noDict // set from the object's own dictionary
			}
			d, err := c.cmp(int(x.v), int(y.v), sub)
			if err != nil || d != nil {
				if d != nil {
					d.path = append(d.path, here(i))
				}
				return d, err
			}
		}
	}
	if len(ta) != len(tb) {
		n := min(len(ta), len(tb))
		return mismatch(n, fmt.Sprintf("%d vs %d tokens", len(ta), len(tb))), nil
	}
	return nil, nil
}

// sameRounded reports whether the constants at ta[i] and tb[i], of the same
// basic floating-point or complex type (the reference before them), are the
// same value of that type. types2 records some constants of a typed context
// exactly and others rounded to the type, depending on parentheses around
// them; the compiler rounds them when it uses them.
func (c *exportCmp) sameRounded(ta, tb []tok, i int) bool {
	if i < 3 || ta[i-1].kind != tRef || tb[i-1].kind != tRef || ta[i-3].kind != tMarker || ta[i-3].m != syncType {
		return false
	}
	ka, kb := c.a.basicKind(int(ta[i-1].v)), c.b.basicKind(int(tb[i-1].v))
	if ka != kb {
		return false
	}
	var prec uint
	switch ka {
	case 13, 15: // float32, complex64
		prec = 24
	case 14, 16: // float64, complex128
		prec = 53
	default:
		return false
	}
	ra, rb := roundConst(ta[i].s, prec), roundConst(tb[i].s, prec)
	return ra != "" && ra == rb
}

// basicKind returns the types2.BasicKind of a Type element that is a basic
// type (Code TypeBasic, Len kind), or -1.
func (u *unified) basicKind(abs int) int {
	ts, err := u.elem(abs)
	if err != nil {
		return -1
	}
	for j := 0; j+2 < len(ts) && j < 2; j++ {
		if ts[j].kind == tMarker && ts[j].m == syncType && ts[j+1].kind == tUint && ts[j+1].v == 0 && ts[j+2].kind == tUint {
			return int(ts[j+2].v)
		}
	}
	return -1
}

// roundConst rounds the numbers of a folded constant ("const num X [num Y]")
// to prec bits.
func roundConst(s string, prec uint) string {
	f := strings.Fields(s)
	if len(f) < 3 || f[0] != "const" {
		return ""
	}
	var out []string
	for k := 1; k+1 < len(f); k += 2 {
		if f[k] != "num" {
			return ""
		}
		r, ok := new(big.Rat).SetString(f[k+1])
		if !ok {
			return ""
		}
		x := new(big.Float).SetPrec(prec).SetMode(big.ToNearestEven).SetRat(r)
		out = append(out, x.Text('p', 0))
	}
	return strings.Join(out, " ")
}

// derived returns the derived types of a dictionary (ObjDict element, Unified
// IR version 4): Object1, Len implicits, Len receiver type params, Len type
// params, a type (Type, Bool, Uint or reference) per type parameter, Len
// derived, a Type reference per derived type.
func (u *unified) derived(abs int) ([]int, error) {
	if d, ok := u.derivedCache[abs]; ok {
		return d, nil
	}
	if u.version != 4 {
		return nil, fmt.Errorf("export data version %d: only version 4 dictionaries are understood", u.version)
	}
	ts, err := u.elem(abs)
	if err != nil {
		return nil, err
	}
	bad := fmt.Errorf("dictionary %s: unexpected layout", u.describe(abs))
	if len(ts) < 4 || ts[1].kind != tUint || ts[2].kind != tUint || ts[3].kind != tUint {
		return nil, bad
	}
	j := 4 + 3*int(ts[2].v+ts[3].v)
	if j >= len(ts) || ts[j].kind != tUint || j+int(ts[j].v) >= len(ts) {
		return nil, bad
	}
	var out []int
	for k := range int(ts[j].v) {
		t := ts[j+1+k]
		if t.kind != tRef || t.sec != secType {
			return nil, bad
		}
		out = append(out, int(t.v))
	}
	u.derivedCache[abs] = out
	return out, nil
}

// context shows the tokens around index i (the token at i in brackets).
func context(ts []tok, i int) string {
	var parts []string
	for j := max(0, i-8); j < min(len(ts), i+4); j++ {
		s := ts[j].String()
		if j == i {
			s = "[" + s + "]"
		}
		parts = append(parts, s)
	}
	if i >= len(ts) {
		parts = append(parts, "[end]")
	}
	return strings.Join(parts, " ")
}
