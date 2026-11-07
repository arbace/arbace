declare void @g()
define void @f() {
 call amdgpu_cs_chain void @g()
 ret void
}
