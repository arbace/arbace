declare i32 @g()
define i32 @f() {
 %r = musttail call i32 @g()
 %q = add i32 %r, 0
 ret i32 %q
}
