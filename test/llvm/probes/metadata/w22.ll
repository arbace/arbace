define i32 @f(i32 %p) {
 %v = add i32 %p, 1, !invariant.group !0
 ret i32 %v
}
!0 = !{}
