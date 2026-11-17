declare void @g(ptr)
define void @f(ptr sret(i32) %x) {
 musttail call void @g(ptr %x)
 ret void
}
