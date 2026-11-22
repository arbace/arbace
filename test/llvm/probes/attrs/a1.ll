; verdict: invalid Attribute 'sret(i32)' applied to incompatible type!
; rule: llvm/lib/IR/Verifier.cpp:2156 sret on an i32 parameter: typeIncompatible marks StructRet for non-pointer types; the check comes after the count/exclusion checks which pass.
define void @f(i32 sret(i32) %p) { ret void }
