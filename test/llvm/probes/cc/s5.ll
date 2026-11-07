declare void @g()
define void @f() {
 call ptx_kernel void @g()
 ret void
}
