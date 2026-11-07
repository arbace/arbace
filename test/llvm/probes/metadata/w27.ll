define void @f(i1 %c) {
 br i1 %c, label %a, label %b, !prof !0
a:
 ret void
b:
 ret void
}
!0 = !{!"branch_weights", i32 1, i32 2}
