package main

import (
	"bufio"
	"fmt"
	"go/ast"
	"go/importer"
	"go/parser"
	"go/token"
	"go/types"
	"io"
	"os"
	"path/filepath"
	"reflect"
	"slices"
	"strconv"
	"strings"
	"sync"
	"time"
)

// dumper holds the state of one package's dump.
type dumper struct {
	cfg   *config
	job   *job
	fset  *token.FileSet
	info  *types.Info
	pkg   *types.Package
	sizes types.Sizes
	w     out
	file  string            // base name of the file being written
	names []*types.TypeName // named types declared in the package, in source order
	dirs  [][2]string       // directives: position, text
}

// importer resolves an import path through the importing package's ImportMap to its export
// data, as listed by go list for the configuration. One per package: types are not shared.
type mappedImporter struct {
	imp       types.Importer
	importMap map[string]string
}

func (m *mappedImporter) Import(path string) (*types.Package, error) {
	if p, ok := m.importMap[path]; ok {
		path = p
	}
	return m.imp.Import(path)
}

func dump(cfg *config, exports *exportMap, j *job) (r result) {
	t0 := time.Now()
	r = result{key: j.key, files: len(j.files)}
	defer func() {
		r.dur = time.Since(t0)
		if e := recover(); e != nil {
			r.status, r.msg = "fail", fmt.Sprint(e)
		}
	}()
	if langTooNew(j.lang) {
		r.status, r.msg = "fail", fmt.Sprintf("language version %s is newer than the pinned %s", j.lang, pinnedLang)
		return r
	}
	if j.pkg != nil && j.pkg.Error != nil {
		r.status, r.msg = "fail", j.pkg.Error.Err
		return r
	}
	d := &dumper{cfg: cfg, job: j, fset: token.NewFileSet(), sizes: types.SizesFor("gc", cfg.goarch)}
	var files []*ast.File
	for _, name := range j.files {
		f, err := parser.ParseFile(d.fset, filepath.Join(j.dir, name), nil, parser.ParseComments|parser.SkipObjectResolution)
		if err != nil {
			r.status, r.msg = "fail", err.Error()
			return r
		}
		if langTooNew(f.GoVersion) {
			r.status, r.msg = "fail", fmt.Sprintf("%s: language version %s is newer than the pinned %s", name, f.GoVersion, pinnedLang)
			return r
		}
		files = append(files, f)
	}
	d.info = &types.Info{
		Types:        map[ast.Expr]types.TypeAndValue{},
		Defs:         map[*ast.Ident]types.Object{},
		Uses:         map[*ast.Ident]types.Object{},
		Selections:   map[*ast.SelectorExpr]*types.Selection{},
		Instances:    map[*ast.Ident]types.Instance{},
		Implicits:    map[ast.Node]types.Object{},
		FileVersions: map[*ast.File]string{},
	}
	var importMap map[string]string
	if j.pkg != nil {
		importMap = j.pkg.ImportMap
	}
	lookup := func(path string) (io.ReadCloser, error) {
		f, ok := exports.export[path]
		if !ok {
			return nil, fmt.Errorf("no export data for %q in this configuration", path)
		}
		return os.Open(f)
	}
	var errs []string
	conf := types.Config{
		GoVersion: j.lang,
		Importer:  &mappedImporter{imp: importer.ForCompiler(d.fset, "gc", lookup), importMap: importMap},
		Sizes:     d.sizes,
		Error: func(err error) {
			if te, ok := err.(types.Error); ok {
				errs = append(errs, d.posFile(te.Pos)+": "+te.Msg)
			} else {
				errs = append(errs, err.Error())
			}
		},
	}
	d.pkg, _ = conf.Check(j.path, d.fset, files, d.info)
	r.errs = len(errs)

	file := outPath(j.key)
	if err := os.MkdirAll(filepath.Dir(file), 0o777); err != nil {
		panic(err)
	}
	fh, err := os.Create(file)
	if err != nil {
		panic(err)
	}
	defer fh.Close()
	bw := bufio.NewWriterSize(fh, 1<<20)
	d.w = out{bw}
	d.write(files, errs)
	if err := bw.Flush(); err != nil {
		panic(err)
	}
	if st, err := fh.Stat(); err == nil {
		r.bytes = st.Size()
	}
	r.status = "ok"
	if len(errs) > 0 {
		r.status = "errors"
		r.msg = errs[0]
	}
	return r
}

func (d *dumper) write(files []*ast.File, errs []string) {
	w, j, c := d.w, d.job, d.cfg
	w.WriteString(";; godump: the typed AST of a Go package for arbace.g2c (doc/go/HELPER-NOTES.md)\n")
	w.WriteString("{:godump 1\n :package ")
	w.str(j.path)
	w.WriteString(" :name ")
	w.str(d.pkg.Name())
	w.WriteString("\n :config {:goos ")
	w.str(c.goos)
	w.WriteString(" :goarch ")
	w.str(c.goarch)
	if c.goarch == "amd64" {
		w.WriteString(" :goamd64 ")
	} else {
		w.WriteString(" :goarm64 ")
	}
	w.str(c.archLevel)
	w.WriteString(" :tags ")
	d.strs(c.tags)
	w.WriteString(" :tool-tags ")
	d.strs(c.toolTags)
	w.WriteString(" :goexperiment ")
	w.str(c.goexperiment)
	w.WriteString(" :cgo false :compiler \"gc\" :toolchain ")
	w.str(c.toolchain)
	w.WriteString(" :go-version ")
	w.str(j.lang)
	w.WriteString(" :word-size ")
	w.int(d.sizes.Sizeof(types.Typ[types.Uintptr]))
	w.WriteString(" :max-align ")
	w.int(d.sizes.Alignof(types.Typ[types.Complex128]))
	w.WriteString("}\n :dir ")
	w.str(relDir(c, j.dir))
	if p := j.pkg; p != nil {
		d.list(" :go-files ", p.GoFiles)
		d.list(" :cgo-files ", p.CgoFiles)
		d.list(" :s-files ", p.SFiles)
		d.list(" :ignored-go-files ", p.IgnoredGoFiles)
		d.list(" :embed-patterns ", p.EmbedPatterns)
		d.list(" :embed-files ", p.EmbedFiles)
	} else {
		d.list(" :go-files ", j.files)
	}
	w.WriteString("\n :imports ")
	var imps []string
	for _, p := range d.pkg.Imports() {
		imps = append(imps, p.Path())
	}
	slices.Sort(imps)
	d.strs(imps)
	w.WriteString("\n :errors ")
	d.strs(errs)
	w.WriteString("\n :files [")
	for i, f := range files {
		d.file = j.files[i]
		if i > 0 {
			w.WriteString("\n ")
		}
		w.WriteString("{:file ")
		w.str(d.file)
		if v := d.info.FileVersions[f]; v != "" && v != j.lang {
			w.WriteString(" :go-version ")
			w.str(v)
		}
		w.WriteString("\n  :ast ")
		d.node(f)
		w.WriteString("\n  :comments [")
		first := true
		for _, g := range f.Comments {
			for _, cm := range g.List {
				if !first {
					w.WriteByte(' ')
				}
				first = false
				w.WriteByte('[')
				d.pos(cm.Slash)
				w.WriteByte(' ')
				w.str(cm.Text)
				w.WriteByte(']')
				if isDirective(cm.Text) {
					d.dirs = append(d.dirs, [2]string{d.posFile(cm.Slash), cm.Text})
				}
			}
		}
		w.WriteString("]}")
	}
	d.file = ""
	w.WriteString("]\n :directives [")
	for i, dv := range d.dirs {
		if i > 0 {
			w.WriteString("\n  ")
		}
		w.WriteByte('[')
		w.str(dv[0])
		w.WriteByte(' ')
		w.str(dv[1])
		w.WriteByte(']')
	}
	w.WriteString("]\n :init-order [")
	for i, in := range d.info.InitOrder {
		if i > 0 {
			w.WriteString("\n  ")
		}
		w.WriteString("{:lhs [")
		for k, v := range in.Lhs {
			if k > 0 {
				w.WriteByte(' ')
			}
			w.WriteByte('[')
			w.str(v.Name())
			w.WriteByte(' ')
			w.str(d.posFile(v.Pos()))
			w.WriteByte(']')
		}
		w.WriteString("] :rhs ")
		w.str(d.posFile(in.Rhs.Pos()))
		w.WriteString("}")
	}
	w.WriteString("]\n :types [")
	for i, tn := range d.names {
		if i > 0 {
			w.WriteString("\n  ")
		}
		d.typeView(tn)
	}
	w.WriteString("]}\n")
}

func isDirective(text string) bool {
	return strings.HasPrefix(text, "//go:") || strings.HasPrefix(text, "//line ") ||
		strings.HasPrefix(text, "/*line ") || strings.HasPrefix(text, "// +build") ||
		strings.HasPrefix(text, "//export ") || strings.HasPrefix(text, "//extern ")
}

func (d *dumper) strs(ss []string) {
	d.w.WriteByte('[')
	for i, s := range ss {
		if i > 0 {
			d.w.WriteByte(' ')
		}
		d.w.str(s)
	}
	d.w.WriteByte(']')
}

func (d *dumper) list(key string, ss []string) {
	if len(ss) > 0 {
		d.w.WriteString(key)
		d.strs(ss)
	}
}

// Positions: "LINE:COL" in the file being written, else "FILE:LINE:COL" (FILE a base name of
// the package's files). Never adjusted by //line directives.
func (d *dumper) posString(p token.Pos, withFile bool) string {
	if !p.IsValid() {
		return ""
	}
	pp := d.fset.PositionFor(p, false)
	base := filepath.Base(pp.Filename)
	if !withFile && base == d.file {
		return strconv.Itoa(pp.Line) + ":" + strconv.Itoa(pp.Column)
	}
	return base + ":" + strconv.Itoa(pp.Line) + ":" + strconv.Itoa(pp.Column)
}

func (d *dumper) pos(p token.Pos) {
	if !p.IsValid() {
		d.w.WriteString("nil")
		return
	}
	d.w.WriteByte('"')
	d.w.WriteString(d.posString(p, false))
	d.w.WriteByte('"')
}

func (d *dumper) posFile(p token.Pos) string { return d.posString(p, true) }

func (d *dumper) qual(p *types.Package) string {
	if p == d.pkg {
		return ""
	}
	return p.Path()
}

func (d *dumper) tstr(t types.Type) string { return types.TypeString(t, d.qual) }

// The generic AST: one form (:kebab-type {:field value ...}) per node, fields in go/ast's
// order, fields at their zero value omitted.

type fieldKind int

const (
	fNode  fieldKind = iota // ast.Node, pointer or interface
	fNodes                  // slice of nodes
	fPos
	fToken
	fChanDir
	fString
	fBool
	fComment // *ast.CommentGroup: written as its position
)

type fieldInfo struct {
	index int
	key   string
	kind  fieldKind
}

type nodeInfo struct {
	head   string
	fields []fieldInfo
}

var nodeInfos sync.Map // reflect.Type -> *nodeInfo

var (
	posType     = reflect.TypeFor[token.Pos]()
	tokenType   = reflect.TypeFor[token.Token]()
	chanDirType = reflect.TypeFor[ast.ChanDir]()
	commentType = reflect.TypeFor[*ast.CommentGroup]()
	nodeIface   = reflect.TypeFor[ast.Node]()
)

// Fields not written: the parser's identifier resolution (Obj, Scope, Unresolved), File.Imports
// (the import specs again) and File.Comments (written beside the tree).
var skipField = map[string]bool{"Obj": true, "Scope": true, "Unresolved": true, "Imports": true, "Comments": true}

func infoOf(t reflect.Type) *nodeInfo {
	if ni, ok := nodeInfos.Load(t); ok {
		return ni.(*nodeInfo)
	}
	ni := &nodeInfo{head: kebab(t.Name())}
	for i := range t.NumField() {
		f := t.Field(i)
		if skipField[f.Name] || !f.IsExported() {
			continue
		}
		fi := fieldInfo{index: i, key: kebab(f.Name)}
		switch {
		case f.Type == posType:
			fi.kind = fPos
		case f.Type == tokenType:
			fi.kind = fToken
		case f.Type == chanDirType:
			fi.kind = fChanDir
		case f.Type == commentType:
			fi.kind = fComment
		case f.Type.Kind() == reflect.String:
			fi.kind = fString
		case f.Type.Kind() == reflect.Bool:
			fi.kind = fBool
		case f.Type.Kind() == reflect.Slice && f.Type.Elem().Implements(nodeIface):
			fi.kind = fNodes
		case f.Type.Implements(nodeIface):
			fi.kind = fNode
		default:
			panic(fmt.Sprintf("godump: field %s.%s of type %s", t.Name(), f.Name, f.Type))
		}
		ni.fields = append(ni.fields, fi)
	}
	nodeInfos.Store(t, ni)
	return ni
}

func kebab(s string) string {
	var b strings.Builder
	for i, r := range s {
		if r >= 'A' && r <= 'Z' {
			if i > 0 {
				b.WriteByte('-')
			}
			b.WriteRune(r + 32)
		} else {
			b.WriteRune(r)
		}
	}
	return b.String()
}

func (d *dumper) node(n ast.Node) {
	w := d.w
	v := reflect.ValueOf(n)
	if v.Kind() == reflect.Pointer && v.IsNil() {
		w.WriteString("nil")
		return
	}
	s := v.Elem()
	ni := infoOf(s.Type())
	d.annotate(n)
	w.WriteString("(:")
	w.WriteString(ni.head)
	w.WriteString(" {")
	first := true
	key := func(k string) {
		if !first {
			w.WriteByte(' ')
		}
		first = false
		w.WriteByte(':')
		w.WriteString(k)
		w.WriteByte(' ')
	}
	for _, f := range ni.fields {
		fv := s.Field(f.index)
		switch f.kind {
		case fPos:
			if p := token.Pos(fv.Int()); p.IsValid() {
				key(f.key)
				d.pos(p)
			}
		case fToken:
			if t := token.Token(fv.Int()); t != token.ILLEGAL {
				key(f.key)
				w.str(t.String())
			}
		case fChanDir:
			key(f.key)
			switch ast.ChanDir(fv.Int()) {
			case ast.SEND:
				w.WriteString(":send")
			case ast.RECV:
				w.WriteString(":recv")
			default:
				w.WriteString(":both")
			}
		case fString:
			if fv.String() != "" {
				key(f.key)
				w.str(fv.String())
			}
		case fBool:
			if fv.Bool() {
				key(f.key)
				w.WriteString("true")
			}
		case fComment:
			if !fv.IsNil() {
				key(f.key)
				d.pos(fv.Interface().(*ast.CommentGroup).Pos())
			}
		case fNode:
			if !fv.IsNil() {
				key(f.key)
				d.node(fv.Interface().(ast.Node))
			}
		case fNodes:
			if !fv.IsNil() {
				key(f.key)
				w.WriteByte('[')
				for i := range fv.Len() {
					if i > 0 {
						w.WriteByte(' ')
					}
					d.node(fv.Index(i).Interface().(ast.Node))
				}
				w.WriteByte(']')
			}
		}
	}
	// declarations start on their own line, to keep lines of a sane length
	w.WriteString("})")
	if _, ok := n.(ast.Decl); ok {
		w.WriteString("\n  ")
	}
}
