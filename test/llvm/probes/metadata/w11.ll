; verdict: invalid align metadata value must be an i64!
; rule: llvm/lib/IR/Verifier.cpp:5870 !align on a pointer load with one operand passes the pointer, load and arity checks, but the operand is i32 8, not an i64 ConstantInt.
define ptr @f(ptr %p) {
 %v = load ptr, ptr %p, !align !0
 ret ptr %v
}
!0 = !{i32 8}
