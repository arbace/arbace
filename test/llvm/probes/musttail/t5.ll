declare void @g(i64, ...)
define void @f(i32 %x) {
 musttail call void (i64, ...) @g(i64 1)
 unreachable
}
