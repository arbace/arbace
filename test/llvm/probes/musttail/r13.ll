declare void @g(ptr)
define void @f(ptr %x) {
 musttail call void @g(ptr %x)
 ret void
}
define void @h() {
 musttail call void @f(ptr null)
 ret void
}
