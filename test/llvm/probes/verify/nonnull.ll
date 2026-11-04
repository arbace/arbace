define void @f(ptr %p) {
  %v = load i32, ptr %p, !nonnull !0
  ret void
}
!0 = !{}
