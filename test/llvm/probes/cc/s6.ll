declare void @g()
define void @f() {
 call x86_intrcc void @g()
 ret void
}
