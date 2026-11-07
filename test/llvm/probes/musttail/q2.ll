declare void @g(i32)
define void @f(i32 %x) {
 musttail call void @g(i32 %x)
 %y = add i32 %x, 1
 ret void
}
