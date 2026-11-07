define void @f() {
  tail call fastcc void @g()
  ret void
}
declare void @g()
