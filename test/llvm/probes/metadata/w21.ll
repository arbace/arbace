define void @f(ptr %p) {
 store i32 0, ptr %p, !invariant.load !0
 ret void
}
!0 = !{}
