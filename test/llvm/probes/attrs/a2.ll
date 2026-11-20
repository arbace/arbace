; verdict: invalid Attribute 'byval(i32)' applied to incompatible type!
; rule: llvm/lib/IR/Verifier.cpp:2156 byval on an i32 parameter: typeIncompatible marks ByVal for non-pointer types.
define void @f(i32 byval(i32) %p) { ret void }
