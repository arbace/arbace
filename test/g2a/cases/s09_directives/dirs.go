package dirs

import (
	"embed"
	_ "unsafe"
)

//go:embed a.txt
//go:embed b.txt
var files embed.FS

//go:embed a.txt
var text string

//go:noinline
//go:nosplit
func f() int { return 1 }

// g is documented.
//
//go:noinline
func g() int { return f() }

//go:linkname nanotime runtime.nanotime
func nanotime() int64

func asm() int

//go:linkname pushed
func pushed() int { return 2 }

//go:generate echo hi

func body() {
//line foo.go:10
	_ = 1
}

var (
	//go:embed b.txt
	more string
)
