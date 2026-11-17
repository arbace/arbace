define void @f(ptr %p) {
  store i32 1, ptr %p, !range !0
  ret void
}
!0 = !{i32 0, i32 4}
