; verdict: invalid intrinsic argument 1 type (matching overload type 0) expected i32, but got i64
; rule: llvm/lib/IR/Verifier.cpp:3299 llvm.smax.i32(i32, i64): second param must match overload type i32; declaration is verified before @f.
declare i32 @llvm.smax.i32(i32, i64)
define i32 @f() {
  %x = call i32 @llvm.smax.i32(i32 1, i64 2)
  ret i32 %x
}
