; verdict: invalid Attributes 'byval', 'inalloca', 'preallocated', 'inreg', 'nest', 'byref', and 'sret' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2098 byref and byval both counted: 2.
define void @f(ptr byref(i32) byval(i32) %p) { ret void }
