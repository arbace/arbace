; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:7964 A standard struct-path access tag (base = access type = scalar "int" -> "omnipotent char" -> root) with an i64 offset 0 on a load; every TBAA check passes.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !tbaa !0
 ret i32 %v
}
!0 = !{!1, !1, i64 0}
!1 = !{!"int", !2, i64 0}
!2 = !{!"omnipotent char", !3, i64 0}
!3 = !{!"Simple C/C++ TBAA"}
