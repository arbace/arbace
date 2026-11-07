declare void @llvm.trap()
define fastcc void @f() {
 musttail call void @llvm.trap()
 ret void
}
