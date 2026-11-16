declare void @g()
define void @f() {
 call spir_kernel void @g()
 ret void
}
