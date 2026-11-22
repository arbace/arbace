; verdict: invalid Invalid user of intrinsic instruction!
; rule: llvm/lib/IR/Verifier.cpp:3284 the declaration llvm.va_start.p0(ptr) comes first; @f's call with no argument has another function type, a non-call use (Function.cpp:987) (corrected after the oracle comparison: the first review took the call's own signature check, which comes later)
declare void @llvm.va_start.p0(ptr)
define void @f(ptr %a) {
  call void @llvm.va_start.p0()
  ret void
}
