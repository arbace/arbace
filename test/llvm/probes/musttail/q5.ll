declare i32 @g(i32)
define i32 @f(i32 %x) {
 %r = musttail call i32 @g(i32 %x)
 ret i32 0
}
