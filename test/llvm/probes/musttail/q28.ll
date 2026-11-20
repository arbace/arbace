; verdict: valid
; rule: - kcfi bundle has one i32 constant input; the verifier has no direct-call restriction for kcfi.
declare void @g(i32)
define void @f(i32 %x) {
 call void @g(i32 %x) [ "kcfi"(i32 0) ]
 ret void
}
