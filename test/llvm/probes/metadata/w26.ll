define i32 @f(i32 %p) {
 %v = add i32 %p, 1, !fpmath !0
 ret i32 %v
}
!0 = !{float 1.0}
