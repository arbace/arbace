; verdict: invalid noalias.addrspace type must be i32!
; rule: llvm/lib/IR/Verifier.cpp:4489 One i64 range on a load: the bound types match each other, but noalias.addrspace requires i32 bounds.
define void @f(ptr %p) {
 %v = load i32, ptr %p, !noalias.addrspace !0
 ret void
}
!0 = !{i64 0, i64 1}
