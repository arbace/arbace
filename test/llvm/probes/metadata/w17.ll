define void @f(ptr %p) {
 store i32 0, ptr %p, !noundef !0
 ret void
}
!0 = !{}
