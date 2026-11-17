declare void @g(ptr)
define void @f(ptr %p) {
 call void @g(ptr sret(i32) %p)
 ret void
}
