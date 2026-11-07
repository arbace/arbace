declare void @g(i32)
define void @f(i32 %x) {
 musttail call fastcc void @g(i32 %x)
 ret void
}
