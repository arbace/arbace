; verdict: invalid Access type node must be a valid scalar type
; rule: llvm/lib/IR/Verifier.cpp:8021 A struct-path tag whose access type !{!"int"} has one operand; IsScalarTBAANodeImpl needs 2 or 3.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !tbaa !0
 ret i32 %v
}
!0 = !{!1, !1, i64 0}
!1 = !{!"int"}
