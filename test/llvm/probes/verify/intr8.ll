define void @f(ptr %a) {
  call void @llvm.memcpy.p0.p0.i64(ptr %a, ptr %a, i64 1, i1 false)
  ret void
}
