; verdict: valid
; rule: - A constant argument is allowed; only prototypes and ABI attributes must match.
declare void @g(i32)
define void @f(i32 %x) {
 musttail call void @g(i32 5)
 ret void
}
