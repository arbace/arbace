; verdict: valid
; rule: - nest is not in the ABI attribute list compared by getParameterABIAttributes.
declare void @g(ptr nest)
define void @f(ptr %x) {
 musttail call void @g(ptr nest %x)
 ret void
}
