declare void @llvm.trap()
define i32 @f() {
 musttail call void @llvm.trap()
 unreachable
}
