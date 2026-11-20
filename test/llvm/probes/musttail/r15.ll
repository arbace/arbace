; verdict: valid
; rule: - An unused alloca before the call is irrelevant.
declare void @g(ptr)
define void @f(ptr %x) {
 %r = alloca i32
 musttail call void @g(ptr %x)
 ret void
}
