define float @f(float %p) {
 %v = fadd float %p, 1.0, !fpmath !0
 ret float %v
}
!0 = !{double 1.0}
