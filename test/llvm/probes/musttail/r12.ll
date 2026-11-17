declare void @g(ptr preallocated(i32))
define void @f(ptr preallocated(i32) %x) {
 musttail call void @g(ptr preallocated(i32) %x)
 ret void
}
