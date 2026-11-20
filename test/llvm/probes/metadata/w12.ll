; verdict: invalid align metadata value must be a power of 2!
; rule: llvm/lib/IR/Verifier.cpp:5873 !align !{i64 3} is a well-formed i64 operand on a pointer load, but 3 is not a power of two.
define ptr @f(ptr %p) {
 %v = load ptr, ptr %p, !align !0
 ret ptr %v
}
!0 = !{i64 3}
