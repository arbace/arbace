declare void @g(i32)
define void @f() {
  call void (i32) @g(i64 1)
  ret void
}
