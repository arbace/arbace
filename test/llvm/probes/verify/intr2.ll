; verdict: invalid intrinsic has incorrect number of args. Expected 0, but got 1
; rule: llvm/lib/IR/Verifier.cpp:3299 Declaration llvm.trap(i32): Intrinsics.cpp:1356.
declare void @llvm.trap(i32)
