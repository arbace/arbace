; verdict: invalid intrinsic argument 0 type (matching overload type 0) expected i32, but got float
; rule: llvm/lib/IR/Verifier.cpp:3299 Return i32 fixes overload 0 = i32; params float do not match.
declare i32 @llvm.smax.i32(float, float)
