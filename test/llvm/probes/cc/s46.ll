declare void @g(ptr, i32)
define void @f(ptr %p) {
 call void @g(ptr inalloca(i32) %p, i32 1)
 ret void
}
