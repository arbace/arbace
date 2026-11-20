; verdict: valid
; rule: - tail (not musttail) call followed by ret of a constant is legal.
declare i32 @g()
define i32 @f() {
 %r = tail call i32 @g()
 ret i32 0
}
