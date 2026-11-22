; verdict: invalid intrinsic return type expected void, but got i32
; rule: llvm/lib/IR/Verifier.cpp:3299 llvm.trap returns void; Intrinsics.cpp:976 message from the return type check.
declare i32 @llvm.trap()
