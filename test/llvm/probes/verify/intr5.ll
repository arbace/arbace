; verdict: invalid intrinsic argument 0 type (matching overload type 0) expected i64, but got i32
; rule: llvm/lib/IR/Verifier.cpp:3299 Return i64 fixes overload 0 = i64; params are i32.
declare i64 @llvm.smax.i32(i32, i32)
define i64 @f() {
  %x = call i64 @llvm.smax.i32(i32 1, i32 2)
  ret i64 %x
}
