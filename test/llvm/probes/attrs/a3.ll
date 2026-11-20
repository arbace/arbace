; verdict: invalid Attribute 'inalloca(i32)' applied to incompatible type!
; rule: llvm/lib/IR/Verifier.cpp:2156 inalloca on i32: typeIncompatible marks InAlloca for non-pointer types, before the last-parameter check.
define void @f(i32 inalloca(i32) %p) { ret void }
