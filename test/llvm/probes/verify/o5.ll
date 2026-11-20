; verdict: invalid llvm.lifetime.start/end can only be used on alloca or poison
; rule: llvm/lib/IR/Verifier.cpp:7019 @g, the first function, calls llvm.lifetime.start.p0 on its argument %a, neither an alloca nor poison (visitIntrinsicCall); @g is visited before the declaration and @f (corrected after the oracle comparison: the first review looked only at @f's call)
define void @g(ptr %a) {
  call void @llvm.lifetime.start.p0(ptr %a)
  ret void
}
declare void @llvm.lifetime.start.p0(ptr)
define void @f(ptr %a) {
  call void @llvm.lifetime.start.p0()
  ret void
}
