; verdict: invalid Intrinsic called with incompatible signature
; rule: llvm/lib/IR/Verifier.cpp:3849 @f's call is a forward reference later resolved by the 4-param declaration; f is visited first and fails.
define void @f(ptr %a) {
  call void @llvm.memcpy.p0.p0.i64(ptr %a, ptr %a, i64 1)
  ret void
}
declare void @llvm.memcpy.p0.p0.i64(ptr, ptr, i64, i1)
