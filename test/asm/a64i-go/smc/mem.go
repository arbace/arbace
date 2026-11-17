package main

import "unsafe"

func unsafeBytes(p uintptr) []byte { return unsafe.Slice((*byte)(unsafe.Pointer(p)), page) }

func addrOf(b []byte) uintptr { return uintptr(unsafe.Pointer(&b[0])) }
