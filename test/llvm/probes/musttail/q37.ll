declare void @g(i32)
define void @f(i32 %x) {
 musttail call void @g(i32 %x)
 ret void
}
define void @h(i32 %x) {
 call void @f(i32 %x)
 ret void
}
