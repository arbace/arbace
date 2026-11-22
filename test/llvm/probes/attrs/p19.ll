; verdict: invalid Attribute 'range(i64 0, 1)' applied to incompatible type!
; rule: llvm/lib/IR/Verifier.cpp:2156 Range bit width 64 differs from i32: typeIncompatible adds Range, reported before the later bit width check.
define void @f(i32 range(i64 0, 1) %p) { ret void }
