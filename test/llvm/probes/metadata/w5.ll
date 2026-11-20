; verdict: invalid The lower limit must be an integer!
; rule: llvm/lib/IR/Verifier.cpp:4480 Two operands (one range), but the lower bound is a float constant, so dyn_extract<ConstantInt> gives null.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !range !0
 ret i32 %v
}
!0 = !{float 0.0, float 5.0}
