; verdict: invalid Invalid user of intrinsic instruction!
; rule: llvm/lib/IR/Verifier.cpp:3284 the declaration is visited before @f; @f's short-form call has its own function type (3 arguments), not the declaration's, so Function::hasAddressTaken (Function.cpp:987) counts it as a non-call use (corrected after the oracle comparison: the first review took the call's own signature check, which comes later)
declare void @llvm.memcpy.p0.p0.i64(ptr, ptr, i64, i1)
define void @f(ptr %a) {
  call void @llvm.memcpy.p0.p0.i64(ptr %a, ptr %a, i64 1)
  ret void
}
