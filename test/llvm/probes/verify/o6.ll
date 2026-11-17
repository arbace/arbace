declare void @llvm.lifetime.start.p0(ptr)
define void @f(ptr %a) {
  call void @llvm.lifetime.start.p0()
  ret void
}
