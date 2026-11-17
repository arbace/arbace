declare void @g(ptr, ...)
define void @f(ptr %p) {
 call void (ptr, ...) @g(ptr %p, ptr sret(i32) %p)
 ret void
}
