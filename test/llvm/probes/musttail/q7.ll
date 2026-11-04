declare void @g(i32)
define void @f(i64 %x) {
 musttail call void @g(i32 1)
 ret void
}
