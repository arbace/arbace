define void @f(ptr %p) {
 %v = load i32, ptr %p, !alias.scope !0
 ret void
}
!0 = !{!1}
!1 = !{!1, !2, !3, !4}
!2 = !{!2, !"d"}
!3 = !{}
!4 = !{}
