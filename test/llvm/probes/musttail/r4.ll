declare void @g(ptr byval(i32))
define void @f(ptr %x) {
 musttail call void @g(ptr byval(i32) %x)
 ret void
}
