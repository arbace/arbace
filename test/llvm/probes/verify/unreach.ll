define i32 @f() {
  ret i32 0
x:
  %y = add i32 %z, 1
  %z = add i32 %y, 1
  ret i32 %z
}
