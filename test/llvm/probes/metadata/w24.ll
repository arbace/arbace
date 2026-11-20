; verdict: invalid fpmath accuracy must have float type
; rule: llvm/lib/IR/Verifier.cpp:5786 The accuracy operand is a double ConstantFP; the semantics check requires IEEEsingle.
define float @f(float %p) {
 %v = fadd float %p, 1.0, !fpmath !0
 ret float %v
}
!0 = !{double 1.0}
