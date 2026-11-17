declare void @g(ptr, ...)
define void @f(ptr %p) {
 call void (ptr, ...) @g(ptr %p, ptr inalloca(i32) %p, i32 1)
 ret void
}
