// Command elfstatic checks that executables are static Linux ELF executables for an architecture,
// with Go's debug/elf: what bin/g2a-build-tests asked of binutils' readelf and of file(1) until
// 2026-10-16 (doc/go/BOX-NOTES.md, "... cgo dropped": the host's C toolchain no longer
// needed).
//
// Usage:
//
//	elfstatic ARCH EXE...
//
// ARCH is amd64 or arm64. Each EXE must be a 64-bit little-endian ELF executable (ET_EXEC) for
// ARCH (EM_X86_64, EM_AARCH64) with no interpreter (no PT_INTERP segment, no .interp section)
// and no dynamic linking (no PT_DYNAMIC segment, no .dynamic section). It prints a line for each,
// "EXE: ELF 64-bit LSB executable, ARCH, statically linked", and exits 1 when one is not.
package main

import (
	"debug/elf"
	"fmt"
	"os"
)

var machines = map[string]elf.Machine{"amd64": elf.EM_X86_64, "arm64": elf.EM_AARCH64}

func main() {
	if len(os.Args) < 3 || machines[os.Args[1]] == elf.EM_NONE {
		fmt.Fprintln(os.Stderr, "usage: elfstatic amd64|arm64 EXE...")
		os.Exit(2)
	}
	status := 0
	for _, exe := range os.Args[2:] {
		if err := check(exe, os.Args[1]); err != nil {
			fmt.Fprintf(os.Stderr, "elfstatic: %s: %v\n", exe, err)
			status = 1
			continue
		}
		fmt.Printf("%s: ELF 64-bit LSB executable, %s, statically linked\n", exe, os.Args[1])
	}
	os.Exit(status)
}

func check(exe, arch string) error {
	f, err := elf.Open(exe)
	if err != nil {
		return err
	}
	defer f.Close()
	switch {
	case f.Class != elf.ELFCLASS64 || f.Data != elf.ELFDATA2LSB:
		return fmt.Errorf("not 64-bit little-endian: %v %v", f.Class, f.Data)
	case f.Type != elf.ET_EXEC:
		return fmt.Errorf("not an executable: %v", f.Type)
	case f.Machine != machines[arch]:
		return fmt.Errorf("for %v, not %s", f.Machine, arch)
	}
	for _, p := range f.Progs {
		if p.Type == elf.PT_INTERP || p.Type == elf.PT_DYNAMIC {
			return fmt.Errorf("not static: a %v segment", p.Type)
		}
	}
	for _, n := range []string{".interp", ".dynamic"} {
		if f.Section(n) != nil {
			return fmt.Errorf("not static: a %s section", n)
		}
	}
	return nil
}
