declare void @llvm.va_start.p0(ptr)
define void @f(ptr %a) {
  call void @llvm.va_start.p0()
  ret void
}
