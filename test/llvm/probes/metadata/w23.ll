; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:5788 !fpmath !{float 1.0} on a float fadd: floating-point result, one operand, IEEE single semantics, finite, nonzero and positive.
define float @f(float %p) {
 %v = fadd float %p, 1.0, !fpmath !0
 ret float %v
}
!0 = !{float 1.0}
