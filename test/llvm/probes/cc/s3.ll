declare void @g()
define void @f() {
 call amdgpu_ps void @g()
 ret void
}
