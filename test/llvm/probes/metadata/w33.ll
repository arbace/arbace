define i32 @f(i32 %x) {
 %y = add i32 %x, 1, !prof !0
 ret i32 %y
}
!0 = !{!"branch_weights", i32 1}
