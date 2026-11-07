declare i32 @g(i32, i32)
define fastcc i32 @f(i32 %x) {
 musttail call i32 @g(i32 %x, i32 %x)
 ret i32 0
}
