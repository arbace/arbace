; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:7968 A call is among the instructions allowed a TBAA tag, and the tag is the standard valid struct-path one.
define void @f(ptr %p) {
 call void @g(), !tbaa !0
 ret void
}
declare void @g()
!0 = !{!1, !1, i64 0}
!1 = !{!"int", !2, i64 0}
!2 = !{!"omnipotent char", !3, i64 0}
!3 = !{!"Simple C/C++ TBAA"}
