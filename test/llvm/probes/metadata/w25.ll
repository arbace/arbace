; verdict: invalid fpmath accuracy not a positive number!
; rule: llvm/lib/IR/Verifier.cpp:5788 float -1.0 has the right type but is negative.
define float @f(float %p) {
 %v = fadd float %p, 1.0, !fpmath !0
 ret float %v
}
!0 = !{float -1.0}
