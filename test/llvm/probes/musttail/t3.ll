declare void @g(i64)
define fastcc void @f(i32 %x) {
 musttail call void @g(i64 1)
 ret void
}
