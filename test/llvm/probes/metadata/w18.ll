; verdict: invalid dereferenceable, dereferenceable_or_null metadata value must be an i64!
; rule: llvm/lib/IR/Verifier.cpp:5228 visitDereferenceableMetadata: pointer result, load, one operand, but the operand is i32 8 rather than i64.
define ptr @f(ptr %p) {
 %v = load ptr, ptr %p, !dereferenceable !0
 ret ptr %v
}
!0 = !{i32 8}
