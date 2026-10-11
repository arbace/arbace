// Command elfsyms cuts a box guest's symbol table to the symbols the monitor
// reads (doc/go/BOX-NOTES.md, S2, amendment BX11).
//
// Usage:
//
//	elfsyms IN OUT SYMBOL...
//
// IN is a 64-bit little-endian ELF executable (a TamaGo image, amd64 or
// arm64) whose .symtab, .strtab and .shstrtab are the last things in the
// file, as Go's linker writes them. OUT is IN with a symbol table of the
// named symbols alone (in IN's order, all of them global), each of which IN
// must define: the loaded segments, the other sections and the section
// headers are IN's, byte for byte, and only the three sections' offsets,
// sizes and the symbol table's first global index change. The monitor
// (package vmm) finds the guest's entry points and knows a TamaGo image by
// their names (whim_vectors, whim_apentry, runtime/goos.RamStart), and parses
// the whole table to find them; Go's own tables (.gopclntab), which the
// guest's stack traces read, are loaded sections and stay. Linking with -s
// would drop the table whole; this keeps what the monitor needs, without
// binutils (whose objcopy here knows x86 only).
package main

import (
	"bytes"
	"debug/elf"
	"encoding/binary"
	"fmt"
	"os"
	"sort"
)

func main() {
	if len(os.Args) < 4 {
		fmt.Fprintln(os.Stderr, "usage: elfsyms IN OUT SYMBOL...")
		os.Exit(2)
	}
	if err := run(os.Args[1], os.Args[2], os.Args[3:]); err != nil {
		fmt.Fprintln(os.Stderr, "elfsyms:", err)
		os.Exit(1)
	}
}

func run(in, out string, names []string) error {
	data, err := os.ReadFile(in)
	if err != nil {
		return err
	}
	f, err := elf.NewFile(bytes.NewReader(data))
	if err != nil {
		return err
	}
	if f.Class != elf.ELFCLASS64 || f.Data != elf.ELFDATA2LSB {
		return fmt.Errorf("%s: not a 64-bit little-endian ELF file", in)
	}
	var symtab, strtab, shstrtab int = -1, -1, -1
	for i, s := range f.Sections {
		if s.Type == elf.SHT_SYMTAB {
			symtab = i
			strtab = int(s.Link)
		}
	}
	shstrtab = int(binary.LittleEndian.Uint16(data[62:])) // e_shstrndx
	if symtab < 0 || strtab <= 0 || strtab >= len(f.Sections) || shstrtab <= 0 || shstrtab >= len(f.Sections) {
		return fmt.Errorf("%s: no symbol table", in)
	}
	// the three sections last in the file, in this order, nothing of the rest after them
	tail := []int{symtab, strtab, shstrtab}
	sort.Slice(tail, func(i, j int) bool { return f.Sections[tail[i]].Offset < f.Sections[tail[j]].Offset })
	start := f.Sections[tail[0]].Offset
	shoff := binary.LittleEndian.Uint64(data[40:])
	shentsize := uint64(binary.LittleEndian.Uint16(data[58:]))
	shnum := uint64(len(f.Sections))
	if shoff+shnum*shentsize > start {
		return fmt.Errorf("%s: the section headers are not before the symbol table", in)
	}
	for i, s := range f.Sections {
		if i == symtab || i == strtab || i == shstrtab || s.Type == elf.SHT_NOBITS || s.Type == elf.SHT_NULL {
			continue
		}
		if s.Offset+s.FileSize > start {
			return fmt.Errorf("%s: section %s is after the symbol table", in, s.Name)
		}
	}
	for _, p := range f.Progs {
		if p.Off+p.Filesz > start {
			return fmt.Errorf("%s: a segment is after the symbol table", in)
		}
	}
	if l := f.Sections[tail[2]]; l.Offset+l.FileSize != uint64(len(data)) {
		return fmt.Errorf("%s: something after the symbol table and the section names", in)
	}

	// the kept symbols, in the table's order
	syms, err := f.Symbols()
	if err != nil {
		return err
	}
	want := map[string]bool{}
	for _, n := range names {
		want[n] = true
	}
	var kept []elf.Symbol
	for _, s := range syms {
		if want[s.Name] {
			if elf.ST_BIND(s.Info) == elf.STB_LOCAL {
				return fmt.Errorf("%s: symbol %s is local", in, s.Name)
			}
			kept = append(kept, s)
			delete(want, s.Name)
		}
	}
	if len(want) > 0 {
		var missing []string
		for n := range want {
			missing = append(missing, n)
		}
		sort.Strings(missing)
		return fmt.Errorf("%s: no symbol %v", in, missing)
	}
	str := []byte{0}
	sym := make([]byte, 24) // the null symbol
	for _, s := range kept {
		var e [24]byte
		binary.LittleEndian.PutUint32(e[0:], uint32(len(str)))
		e[4] = s.Info
		e[5] = s.Other
		binary.LittleEndian.PutUint16(e[6:], uint16(s.Section))
		binary.LittleEndian.PutUint64(e[8:], s.Value)
		binary.LittleEndian.PutUint64(e[16:], s.Size)
		sym = append(sym, e[:]...)
		str = append(append(str, s.Name...), 0)
	}
	shstr := data[f.Sections[shstrtab].Offset : f.Sections[shstrtab].Offset+f.Sections[shstrtab].FileSize]

	// the file: everything before the tables as it is, then the three, each aligned as its
	// section header says
	o := append([]byte(nil), data[:start]...)
	place := func(i int, b []byte) {
		a := f.Sections[i].Addralign
		for a > 1 && uint64(len(o))%a != 0 {
			o = append(o, 0)
		}
		h := shoff + uint64(i)*shentsize
		binary.LittleEndian.PutUint64(o[h+24:], uint64(len(o))) // sh_offset
		binary.LittleEndian.PutUint64(o[h+32:], uint64(len(b))) // sh_size
		o = append(o, b...)
	}
	place(symtab, sym)
	place(strtab, str)
	place(shstrtab, shstr)
	// sh_info of the symbol table: the index of its first global symbol, here 1
	binary.LittleEndian.PutUint32(o[shoff+uint64(symtab)*shentsize+44:], 1)

	// check: the result reads back with the kept symbols
	g, err := elf.NewFile(bytes.NewReader(o))
	if err != nil {
		return err
	}
	gs, err := g.Symbols()
	if err != nil || len(gs) != len(kept) {
		return fmt.Errorf("the result's symbols do not read back: %v", err)
	}
	for i, s := range gs {
		if s.Name != kept[i].Name || s.Value != kept[i].Value {
			return fmt.Errorf("the result's symbol %d is %s, not %s", i, s.Name, kept[i].Name)
		}
	}
	return os.WriteFile(out, o, 0o755)
}
