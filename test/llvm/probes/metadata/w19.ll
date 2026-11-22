; verdict: invalid dereferenceable, dereferenceable_or_null take one operand!
; rule: llvm/lib/IR/Verifier.cpp:5223 !dereferenceable on a pointer load with two operands (i64 8, i64 9): the pointer and load checks pass, the one-operand check fails.
define ptr @f(ptr %p) {
 %v = load ptr, ptr %p, !dereferenceable !0
 ret ptr %v
}
!0 = !{i64 8, i64 9}
