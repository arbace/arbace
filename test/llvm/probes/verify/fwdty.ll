define i32 @f() {
  br label %b
a:
  ret i32 %x
b:
  %x = add i64 1, 2
  br label %a
}
