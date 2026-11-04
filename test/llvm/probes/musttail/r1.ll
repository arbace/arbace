declare void @g(ptr sret(i32))
define void @f(ptr %x) {
 musttail call void @g(ptr sret(i32) %x)
 ret void
}
