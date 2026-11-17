declare ptr @g()
define i32 @f() {
 %r = musttail call ptr @g()
 ret i32 0
}
