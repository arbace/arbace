; verdict: invalid Access type node must be a valid scalar type
; rule: llvm/lib/IR/Verifier.cpp:8021 !{!1, !1} has two operands, so at the end of parsing UpgradeTBAANode (llvm/lib/IR/AutoUpgrade.cpp:5994) wraps it as !{!0, !0, i64 0}; the access type is then !0, whose first operand is a node rather than a string, so it is not a scalar type node.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !tbaa !0
 ret i32 %v
}
!0 = !{!1, !1}
!1 = !{!"int", !2, i64 0}
!2 = !{!"omnipotent char", !3, i64 0}
!3 = !{!"Simple C/C++ TBAA"}
