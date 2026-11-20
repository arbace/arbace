; verdict: valid
; rule: - noreturn on a call site is allowed and not checked by verifyMustTailCall.
declare void @g(ptr)
define void @f(ptr %x) {
 musttail call void @g(ptr %x) #0
 ret void
}
attributes #0 = { noreturn }
