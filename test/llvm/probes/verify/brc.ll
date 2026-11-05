define void @f(i32 %c) {
  br i32 %c, label %a, label %a
a:
  ret void
}
