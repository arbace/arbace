; verdict: invalid intrinsic argument 1 type expected i1, but got i32
; rule: llvm/lib/IR/Verifier.cpp:3299 llvm.ctlz.i32 with 2 params is not upgraded (only 1-arg form); second param must be i1.
declare i32 @llvm.ctlz.i32(i32, i32)
