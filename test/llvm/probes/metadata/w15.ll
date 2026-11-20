; verdict: invalid noundef metadata must be empty
; rule: llvm/lib/IR/Verifier.cpp:5838 !noundef on a load is allowed, but its node !{i32 1} is not empty.
define ptr @f(ptr %p) {
 %v = load ptr, ptr %p, !noundef !0
 ret ptr %v
}
!0 = !{i32 1}
