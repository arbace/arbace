; verdict: invalid Invalid user of intrinsic instruction!
; rule: llvm/lib/IR/Verifier.cpp:3284 module order @g, the declaration (its forward reference resolved after @g), @f: @g's call matches; at the declaration, @f's 3-argument call has another function type, a non-call use (Function.cpp:987) (corrected after the oracle comparison: the first review took the call's own signature check, which comes later)
define void @g(ptr %a) {
  call void @llvm.memcpy.p0.p0.i64(ptr %a, ptr %a, i64 1, i1 false)
  ret void
}
declare void @llvm.memcpy.p0.p0.i64(ptr, ptr, i64, i1)
define void @f(ptr %a) {
  call void @llvm.memcpy.p0.p0.i64(ptr %a, ptr %a, i64 1)
  ret void
}
