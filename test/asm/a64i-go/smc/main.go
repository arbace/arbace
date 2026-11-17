// a64i's Go program (queue 303): code written at run time, run, rewritten and run again, as a JIT does; a64i's decoded
// instructions (a64i's per-page cache, its word checked at every fetch, the page's bytes kept while the address space's Gen is
// unchanged) must follow every change: the same word rewritten between calls, code that rewrites its own next instruction, a page
// unmapped and mapped again at its address, a page made writable and executable again, and a page that maps a file over itself
// with a system call it makes and runs on in the new mapping. Each run's result printed; the M2's run is the reference.
package main

import (
	"fmt"
	"os"
	"syscall"
)

// call runs the code at addr with x0 a0 and x1 a1 (call_arm64.s): its x0
func call(addr, a0, a1 uintptr) uint64

// sync makes n bytes at addr written as data visible to instruction fetch (dc cvau, dsb ish, ic ivau, dsb ish, isb)
func sync(addr, n uintptr)

const (
	ret   = 0xd65f03c0
	movz0 = 0xd2800000 // movz x0, #imm: | imm<<5
	page  = 4096
)

func movz(imm uint32) uint32 { return movz0 | imm<<5 }

func put(b []byte, off int, words ...uint32) {
	for i, w := range words {
		o := off + 4*i
		b[o], b[o+1], b[o+2], b[o+3] = byte(w), byte(w>>8), byte(w>>16), byte(w>>24)
	}
}

func mapRWX(addr uintptr, fixed bool) []byte {
	flags := uintptr(syscall.MAP_PRIVATE | syscall.MAP_ANON)
	if fixed {
		flags |= syscall.MAP_FIXED
	}
	r, _, e := syscall.Syscall6(syscall.SYS_MMAP, addr, page, syscall.PROT_READ|syscall.PROT_WRITE|syscall.PROT_EXEC, flags, ^uintptr(0), 0)
	if e != 0 {
		fmt.Println("mmap:", e)
		os.Exit(1)
	}
	return unsafeBytes(r)
}

func main() {
	b := mapRWX(0, false)
	a := addrOf(b)
	run := func(what string, a0, a1 uintptr) {
		sync(a, page)
		fmt.Printf("%s: %d\n", what, call(a, a0, a1))
	}
	// 1. written and run, then the same word rewritten and run again
	put(b, 0, movz(1), ret)
	run("written", 0, 0)
	put(b, 0, movz(2))
	run("rewritten", 0, 0)
	// 2. code that rewrites its own next instruction (its immediate one more each run) and runs it
	put(b, 0, 0x10000122, 0xb9400041, 0x11008021, 0xb9000041, 0xd50b7b22, 0xd5033b9f, 0xd50b7522, 0xd5033b9f, 0xd5033fdf, movz(3), ret)
	for i := 0; i < 3; i++ {
		run("self-modified", 0, 0)
	}
	// 3. unmapped, mapped again at its address (MAP_FIXED), other code written
	if _, _, e := syscall.Syscall(syscall.SYS_MUNMAP, a, page, 0); e != 0 {
		fmt.Println("munmap:", e)
		os.Exit(1)
	}
	b = mapRWX(a, true)
	put(b, 0, movz(7), ret)
	run("mapped again", 0, 0)
	// 4. made writable only, rewritten, made executable again
	if err := syscall.Mprotect(b, syscall.PROT_READ|syscall.PROT_WRITE); err != nil {
		fmt.Println("mprotect:", err)
		os.Exit(1)
	}
	put(b, 0, movz(8))
	if err := syscall.Mprotect(b, syscall.PROT_READ|syscall.PROT_WRITE|syscall.PROT_EXEC); err != nil {
		fmt.Println("mprotect:", err)
		os.Exit(1)
	}
	run("protected again", 0, 0)
	// 5. a file mapped over the running page by the code itself (mmap(x0, 4096, RWX, MAP_PRIVATE|MAP_FIXED, x1, 0)): the
	// instruction after the svc is the file's (movz x0, #9), not the page's (#10)
	code := []uint32{0xaa0103e4, 0xd2820001, 0xd28000e2, 0xd2800243, 0xd2800005, 0xd2801bc8, 0xd4000001}
	put(b, 0, code...)
	put(b, 4*len(code), movz(10), ret)
	f := make([]byte, page)
	copy(f, b)
	put(f, 4*len(code), movz(9))
	if err := os.WriteFile("code.bin", f, 0o644); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
	fd, err := syscall.Open("code.bin", syscall.O_RDONLY, 0)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
	os.Remove("code.bin")
	run("mapped over itself", a, uintptr(fd))
	syscall.Close(fd)
	fmt.Println("done.")
}
