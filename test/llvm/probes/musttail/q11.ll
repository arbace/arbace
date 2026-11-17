declare void @g(i32)
define fastcc void @f(i32 %x) {
 musttail call void @g(i32 %x)
 ret void
}
