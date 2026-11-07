define i32 @f(i64 %c) {
  %x = zext i64 %c to i32
  ret i32 %x
}
