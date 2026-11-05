define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !noundef !0
 ret i32 %v
}
!0 = !{}
