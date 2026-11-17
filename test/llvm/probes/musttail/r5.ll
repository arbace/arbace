declare void @g(ptr byval(i32))
define void @f(ptr byval(i64) %x) {
 musttail call void @g(ptr byval(i32) %x)
 ret void
}
