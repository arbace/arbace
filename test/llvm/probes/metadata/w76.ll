; verdict: invalid scope must have two or three operands
; rule: llvm/lib/IR/Verifier.cpp:5496 The scope !{!"x"} has one operand.
define void @f(ptr %p) {
 %v = load i32, ptr %p, !alias.scope !0
 ret void
}
!0 = !{!1}
!1 = !{!"x"}
