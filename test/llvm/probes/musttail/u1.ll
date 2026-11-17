declare void @llvm.trap()
define void @f(...) {
 musttail call void @llvm.trap()
 ret void
}
