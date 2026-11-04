// libtest_cover.go: bin/pyi cover's hook (queue 201, PY7; doc/python/PYTHON-SURVEY.md, "As built: PY7 (the check)"). Copied
// into the cover build's printed runtime only (bin/pyi cover run: the AOT module of .tmp/repr-python/pyi/aot-cover, built with
// -cover -covermode=atomic -coverpkg=arbace/py), never into go/arbace/py: goi's table has no runtime/coverage, and no other
// build sees it. It adds the built-in module _pyi_cover, whose flush(ID) writes the counters of the runtime's blocks the
// test ID reached into PYI_LIB_TEST_COVER_TESTS/N (N its number, ID in N/id, the meta-data file written once into
// PYI_LIB_TEST_COVER_TESTS/meta and hard-linked: tools/covsubset blocks reads each directory alone) and clears them, so each
// test method has its own block set (test/python/libtest_run.py calls it after each test).
package py

import (
	"fmt"
	"os"
	"path/filepath"
	"runtime/coverage"
	"sync"
)

var coverMu sync.Mutex
var coverN int

func coverFlush(id string) error {
	coverMu.Lock()
	defer coverMu.Unlock()
	base := os.Getenv("PYI_LIB_TEST_COVER_TESTS")
	if base == "" {
		return nil
	}
	meta := filepath.Join(base, "meta")
	if _, err := os.Stat(meta); err != nil {
		if err := os.MkdirAll(meta, 0o755); err != nil {
			return err
		}
		if err := coverage.WriteMetaDir(meta); err != nil {
			return err
		}
	}
	coverN++
	dir := filepath.Join(base, fmt.Sprintf("%d-%d", os.Getpid(), coverN))
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return err
	}
	ms, _ := filepath.Glob(filepath.Join(meta, "covmeta.*"))
	for _, m := range ms {
		os.Link(m, filepath.Join(dir, filepath.Base(m)))
	}
	if err := os.WriteFile(filepath.Join(dir, "id"), []byte(id+"\n"), 0o644); err != nil {
		return err
	}
	if err := coverage.WriteCountersDir(dir); err != nil {
		return err
	}
	return coverage.ClearCounters()
}

func init() {
	builtinModules["_pyi_cover"] = func(in *Interp) *Module {
		d := NewDict()
		d.SetStr("__name__", NewStr("_pyi_cover"))
		d.SetStr("flush", &Builtin{Name: "flush", Fn: func(args []Object, kw []*Str) (Object, error) {
			if len(args) == 1 {
				if s, ok := args[0].(*Str); ok {
					if err := coverFlush(s.String()); err != nil {
						return nil, fmt.Errorf("_pyi_cover.flush: %v", err)
					}
				}
			}
			return None, nil
		}})
		return &Module{Name: "_pyi_cover", Dict: d}
	}
}
