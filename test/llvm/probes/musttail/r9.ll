declare i32 @g(i32 returned)
define i32 @f(i32 %x) {
 %r = musttail call i32 @g(i32 returned %x)
 ret i32 %r
}
