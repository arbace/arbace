; verdict: invalid nonnull metadata must be empty
; rule: llvm/lib/IR/Verifier.cpp:5833 !nonnull on a pointer load is allowed, but its node !{i32 1} has an operand.
define ptr @f(ptr %p) {
 %v = load ptr, ptr %p, !nonnull !0
 ret ptr %v
}
!0 = !{i32 1}
