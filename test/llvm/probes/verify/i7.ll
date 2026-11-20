; verdict: invalid intrinsic argument 0 type (overload type 0) expected any pointer type, but got i32
; rule: llvm/lib/IR/Verifier.cpp:3299 llvm.stacksave.p0 is fine; llvm.stackrestore.p0(i32): anyptr_ty requires a pointer.
declare ptr @llvm.stacksave.p0()
declare void @llvm.stackrestore.p0(i32)
