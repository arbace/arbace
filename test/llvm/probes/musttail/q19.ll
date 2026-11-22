; verdict: valid
; rule: - nounwind as a call function attribute is legal and not checked by verifyMustTailCall.
declare void @g(i32)
define void @f(i32 %x) {
 musttail call void @g(i32 %x) nounwind
 ret void
}
