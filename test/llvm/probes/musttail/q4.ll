declare i32 @g(i32)
define void @f(i32 %x) {
 musttail call i32 @g(i32 %x)
 ret void
}
