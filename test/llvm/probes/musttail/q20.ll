; verdict: valid
; rule: - tail (not musttail) call with a single deopt bundle is legal.
declare void @g(i32)
define void @f(i32 %x) {
 tail call void @g(i32 %x) [ "deopt"() ]
 ret void
}
