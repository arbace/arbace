; verdict: valid
; rule: - sret on a call-site argument of a non-vararg call is allowed.
declare void @g(ptr)
define void @f(ptr %p) {
 call void @g(ptr sret(i32) %p)
 ret void
}
