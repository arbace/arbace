declare void @g()
define void @f() {
  %x = call void @g()
  ret void
}
