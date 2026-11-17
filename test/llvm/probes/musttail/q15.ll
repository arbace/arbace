declare void @g(ptr sret(i32))
define void @f(ptr sret(i32) %x) {
 musttail call void @g(ptr sret(i32) %x)
 ret void
}
