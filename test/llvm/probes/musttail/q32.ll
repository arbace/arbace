; verdict: valid
; rule: - Two plain tail calls are unconstrained.
declare void @g(i32)
define void @f(i32 %x) {
 tail call void @g(i32 %x)
 tail call void @g(i32 %x)
 ret void
}
