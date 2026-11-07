declare void @llvm.trap()
define void @f(i32 %x) {
 musttail call void @llvm.trap()
 ret void
}
