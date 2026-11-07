declare void @g(ptr, i32)
define void @f(ptr %p) {
 call void @g(ptr preallocated(i32) %p, i32 1)
 ret void
}
