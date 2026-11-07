declare void @g()
define void @f() {
 call amdgpu_kernel void @g()
 ret void
}
