; verdict: invalid Intrinsic called with incompatible signature
; rule: llvm/lib/IR/Verifier.cpp:3849 Zero-arg call to lifetime.start.p0(ptr).
declare void @llvm.lifetime.start.p0(ptr)
define void @f(ptr %a) {
  call void @llvm.lifetime.start.p0()
  ret void
}
