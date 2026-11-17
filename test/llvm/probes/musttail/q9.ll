declare void @g(i32, ...)
define void @f(i32 %x) {
 musttail call void (i32, ...) @g(i32 %x)
 ret void
}
