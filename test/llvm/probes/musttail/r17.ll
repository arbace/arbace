declare i32 @g()
define i32 @f() {
 %r = tail call i32 @g()
 ret i32 0
}
