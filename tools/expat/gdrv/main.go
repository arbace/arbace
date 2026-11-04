// The Go side of tools/expat/check.sh's Go/C differential (queue 202e): cdrv.c's events and positions.
package main

import (
	"fmt"
	"os"
	"strconv"

	. "arbace/expat"
)

var P XML_Parser

func pos(ev string) {
	fmt.Printf("%s %d %d %d", ev, XML_GetCurrentLineNumber(P), XML_GetCurrentColumnNumber(P), XML_GetCurrentByteIndex(P))
}
func s(x Ptr) {
	if x.IsNull() {
		fmt.Print(" NULL")
	} else {
		fmt.Printf(" [%s]", x.String())
	}
}
func sl(x Ptr, n int32) { fmt.Printf(" [%s]", string(x.Bytes(int(n)))) }
func model(m *XML_Content) {
	fmt.Printf("(%d %d", m.Type(), m.Quant())
	s(m.Name())
	for i := 0; i < int(m.Numchildren()); i++ {
		model(m.Child(i))
	}
	fmt.Print(")")
}

func main() {
	data, _ := os.ReadFile(os.Args[1])
	ns, _ := strconv.Atoi(os.Args[2])
	chunk, _ := strconv.Atoi(os.Args[3])
	dflt, _ := strconv.Atoi(os.Args[4])
	if ns != 0 {
		P = XML_ParserCreateNS(Ptr{}, '!')
	} else {
		P = XML_ParserCreate(Ptr{})
	}
	XML_SetHashSalt(P, 1234567)
	XML_SetElementHandler(P, func(u any, n Ptr, a []Ptr) {
		pos("start")
		s(n)
		for i := 0; !a[i].IsNull(); i++ {
			s(a[i])
		}
		fmt.Printf(" spec=%d\n", XML_GetSpecifiedAttributeCount(P))
	}, func(u any, n Ptr) { pos("end"); s(n); fmt.Println() })
	XML_SetCharacterDataHandler(P, func(u any, x Ptr, n int32) { pos("chars"); sl(x, n); fmt.Println() })
	XML_SetProcessingInstructionHandler(P, func(u any, t, d Ptr) { pos("pi"); s(t); s(d); fmt.Println() })
	XML_SetCommentHandler(P, func(u any, d Ptr) { pos("comment"); s(d); fmt.Println() })
	XML_SetCdataSectionHandler(P, func(u any) { pos("scdata"); fmt.Println() }, func(u any) { pos("ecdata"); fmt.Println() })
	if dflt != 0 {
		XML_SetDefaultHandlerExpand(P, func(u any, x Ptr, n int32) { pos("default"); sl(x, n); fmt.Println() })
	}
	XML_SetDoctypeDeclHandler(P, func(u any, n, sy, pu Ptr, h int32) { pos("sdoctype"); s(n); s(sy); s(pu); fmt.Printf(" %d\n", h) },
		func(u any) { pos("edoctype"); fmt.Println() })
	XML_SetElementDeclHandler(P, func(u any, n Ptr, m *XML_Content) {
		pos("eldecl")
		s(n)
		fmt.Print(" ")
		model(m)
		fmt.Println()
		XML_FreeContentModel(P, m)
	})
	XML_SetAttlistDeclHandler(P, func(u any, e, a, t, d Ptr, r int32) { pos("attdecl"); s(e); s(a); s(t); s(d); fmt.Printf(" %d\n", r) })
	XML_SetEntityDeclHandler(P, func(u any, n Ptr, pe int32, v Ptr, vl int32, b, sy, pu, no Ptr) {
		pos("entdecl")
		s(n)
		fmt.Printf(" %d", pe)
		if !v.IsNull() {
			sl(v, vl)
		} else {
			fmt.Print(" NULL")
		}
		s(b)
		s(sy)
		s(pu)
		s(no)
		fmt.Println()
	})
	XML_SetXmlDeclHandler(P, func(u any, v, e Ptr, st int32) { pos("xmldecl"); s(v); s(e); fmt.Printf(" %d\n", st) })
	XML_SetNotationDeclHandler(P, func(u any, n, b, sy, pu Ptr) { pos("notation"); s(n); s(b); s(sy); s(pu); fmt.Println() })
	XML_SetNamespaceDeclHandler(P, func(u any, p, uri Ptr) { pos("sns"); s(p); s(uri); fmt.Println() },
		func(u any, p Ptr) { pos("ens"); s(p); fmt.Println() })
	XML_SetNotStandaloneHandler(P, func(u any) int32 { pos("notstandalone"); fmt.Println(); return 1 })
	XML_SetExternalEntityRefHandler(P, func(p XML_Parser, c, b, sy, pu Ptr) int32 {
		pos("extref")
		s(c)
		s(b)
		s(sy)
		s(pu)
		fmt.Println()
		return 1
	})
	XML_SetSkippedEntityHandler(P, func(u any, n Ptr, pe int32) { pos("skipped"); s(n); fmt.Printf(" %d\n", pe) })
	XML_SetParamEntityParsing(P, XML_PARAM_ENTITY_PARSING_UNLESS_STANDALONE)
	buf := Ptr{B: data}
	st := XML_STATUS_OK
	n := len(data)
	if chunk <= 0 {
		st = XML_Parse(P, buf, int32(n), 1)
	} else {
		for i := 0; i < n && st == XML_STATUS_OK; i += chunk {
			m := chunk
			if n-i < chunk {
				m = n - i
			}
			st = XML_Parse(P, buf.Add(i), int32(m), 0)
		}
		if st == XML_STATUS_OK {
			st = XML_Parse(P, buf, 0, 1)
		}
	}
	if st == XML_STATUS_ERROR {
		c := XML_GetErrorCode(P)
		fmt.Printf("error %d %d %d %d [%s]\n", c, XML_GetErrorLineNumber(P), XML_GetErrorColumnNumber(P), XML_GetErrorByteIndex(P), XML_ErrorString(c).String())
	} else {
		fmt.Println("ok")
	}
	XML_ParserFree(P)
}
