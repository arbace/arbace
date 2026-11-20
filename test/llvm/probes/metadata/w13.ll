; verdict: invalid align takes one operand!
; rule: llvm/lib/IR/Verifier.cpp:5868 !align !{} on a pointer load passes the pointer-type and load checks, then fails the one-operand check.
define ptr @f(ptr %p) {
 %v = load ptr, ptr %p, !align !0
 ret ptr %v
}
!0 = !{}
