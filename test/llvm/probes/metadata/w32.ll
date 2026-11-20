; verdict: invalid !prof annotations should have no less than 2 operands
; rule: llvm/lib/IR/Verifier.cpp:5276 !{!"branch_weights"} has one operand: it passes the at-least-1 and name checks, then fails the at-least-2 check before the branch_weights branch.
define void @f(i1 %c) {
 br i1 %c, label %a, label %b, !prof !0
a:
 ret void
b:
 ret void
}
!0 = !{!"branch_weights"}
