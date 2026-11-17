declare void @g()
define void @f() {
 call fastcc void @g()
 ret void
}
