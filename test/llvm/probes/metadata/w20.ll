; verdict: invalid dereferenceable, dereferenceable_or_null take one operand!
; rule: llvm/lib/IR/Verifier.cpp:5223 !dereferenceable_or_null !{} on a pointer load has zero operands.
define ptr @f(ptr %p) {
 %v = load ptr, ptr %p, !dereferenceable_or_null !0
 ret ptr %v
}
!0 = !{}
