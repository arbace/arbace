; verdict: valid
; rule: - The verifier has no check against passing an alloca pointer to a musttail call.
declare void @g(ptr)
define void @f(ptr %x) {
 %a = alloca i32
 musttail call void @g(ptr %a)
 ret void
}
