define void @f(ptr %p) {
 %v = load i32, ptr %p, !noalias.addrspace !0
 ret void
}
!0 = !{i64 0, i64 1}
