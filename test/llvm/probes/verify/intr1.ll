declare void @llvm.trap(i32)
define void @f() {
  call void @llvm.trap(i32 1)
  ret void
}
