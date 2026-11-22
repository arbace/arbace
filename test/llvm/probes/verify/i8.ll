; verdict: invalid intrinsic argument 0 type expected i1, but got i32
; rule: llvm/lib/IR/Verifier.cpp:3299 llvm.assume takes i1.
declare void @llvm.assume(i32)
