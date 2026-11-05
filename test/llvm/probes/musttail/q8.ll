declare void @g(i32)
define void @f(i32 %x, i32 %y) {
 musttail call void @g(i32 %x)
 ret void
}
