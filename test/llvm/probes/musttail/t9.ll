declare void @g(ptr sret(i32))
define void @f(ptr %x, i32 %y) {
 musttail call void @g(ptr sret(i32) %x)
 unreachable
}
