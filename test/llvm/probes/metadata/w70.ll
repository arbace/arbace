define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !tbaa !0
 ret i32 %v
}
!0 = !{!1, !1, i64 0}
!1 = !{!"int"}
