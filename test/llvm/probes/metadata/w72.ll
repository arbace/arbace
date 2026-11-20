; verdict: invalid Access type node must be a valid scalar type
; rule: llvm/lib/IR/Verifier.cpp:8021 The access type !1 has 4 operands (old format, since operand 0 is a string), and a scalar type node must have 2 or 3.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !tbaa !0
 ret i32 %v
}
!0 = !{!1, !1, i64 0}
!1 = !{!"int", !2, i64 0, i64 4}
!2 = !{!"omnipotent char", !3, i64 0}
!3 = !{!"Simple C/C++ TBAA"}
