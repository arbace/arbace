declare void @g(ptr)
define void @f(ptr %x) {
 musttail call void @g(ptr %x)
 ret void
}
define void @h(ptr %x) {
 musttail call void @f(ptr %x)
 musttail call void @g(ptr %x)
 ret void
}
