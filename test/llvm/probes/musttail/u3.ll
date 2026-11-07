declare void @llvm.trap()
define i32 @f() {
 musttail call void @llvm.trap()
 ret i32 0
}
