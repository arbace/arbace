; verdict: invalid intrinsic has incorrect number of args. Expected 2, but got 3
; rule: llvm/lib/IR/Verifier.cpp:3299 llvm.smax.i32 declared with 3 params; return type i32 matches anyint first (Intrinsics.cpp:1356).
declare i32 @llvm.smax.i32(i32, i32, i32)
