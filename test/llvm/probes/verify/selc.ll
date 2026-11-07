define i32 @f(i32 %c) {
  %s = select i32 %c, i32 1, i32 2
  ret i32 %s
}
