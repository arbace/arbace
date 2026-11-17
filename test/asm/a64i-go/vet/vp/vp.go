// Package vp holds one mistake for each of go vet's analyzers that a
// package without cgo, assembly or tests can show (a64i's go-check, AR21).
package vp

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"os/signal"
	"strings"
	"sync"
	"sync/atomic"
	"time"
	"unsafe"
)

// T has a lock, a bad tag and a String method with a wrong signature.
type T struct {
	mu   sync.Mutex
	N    int    `json:"n"`
	M    int    `json:"n"`
	Name string `xml:name`
}

// ReadByte has the wrong signature for io.ByteReader.
func (t *T) ReadByte() byte { return 0 }

// Lock copies the lock.
func Lock(t T) T { return t }

// Printf's misuses.
func Printf(x int, s string) string {
	fmt.Printf("%d %s\n", s, x)
	fmt.Println("no verbs %d", x)
	_ = fmt.Sprintf("%v")
	return fmt.Sprintf("%.*d", x)
}

// Wrap is a printf wrapper: its fact is exported.
func Wrap(format string, args ...any) { fmt.Printf(format, args...) }

// UseWrap misuses the wrapper.
func UseWrap() { Wrap("%s", 1) }

// Others shows the rest.
func Others(p *int, f func(), xs []int, ctx context.Context, c chan os.Signal) {
	xs = append(xs)
	x := 1
	x = x
	var n int64
	n = atomic.AddInt64(&n, 1)
	if *p == 1 || *p == 1 {
	}
	if f == nil || Others == nil {
	}
	_ = x << 64
	sig := make(chan os.Signal)
	signal.Notify(sig, os.Interrupt)
	ctx2, _ := context.WithCancel(ctx)
	_ = ctx2
	_ = string(rune(x)) + string(x)
	_ = time.Now().Format("2006-02-01")
	var err error
	var pe os.PathError
	_ = errors.As(err, pe)
	var v struct{ A int }
	_ = json.Unmarshal(nil, v)
	fmt.Sprintf("unused %d", x)
	_ = strings.ToUpper
	defer fmt.Println(time.Since(time.Now()))
	var u uintptr
	_ = unsafe.Pointer(u)
	var i interface{ M() }
	_ = i.(interface{ M() int })
	var wg sync.WaitGroup
	go func() {
		wg.Add(1)
		defer wg.Done()
	}()
	wg.Wait()
	return
	fmt.Println("unreachable")
}
