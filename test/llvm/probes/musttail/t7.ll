declare i32 @g(i64)
define fastcc void @f(i32 %x) {
 musttail call i32 @g(i64 1)
 unreachable
}
